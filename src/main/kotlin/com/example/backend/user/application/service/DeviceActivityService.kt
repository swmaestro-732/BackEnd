package com.example.backend.user.application.service

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.user.application.port.inbound.DeviceActivityUseCase
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionOperations
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * 기기의 그날(KST) 활동을 기록한다. 이미 기록한 (기기, 로그인 여부) 는 메모리에서 걸러 요청마다 DB 에 쓰지 않는다.
 * 쓰기는 전용 실행기에서 해 요청 스레드가 DB 커넥션을 기다리지 않는다.
 * 인스턴스가 여러 대면 인스턴스마다 한 번씩 쓰지만 PK 가 중복을 막는다.
 */
@Service
class DeviceActivityService(
    private val devicePersistencePort: DevicePersistencePort,
    private val userPersistencePort: UserPersistencePort,
    private val transactionOperations: TransactionOperations,
    private val clock: Clock,
    @Qualifier(AsyncConfig.DEVICE_ACTIVITY_EXECUTOR) private val deviceActivityExecutor: TaskExecutor,
) : DeviceActivityUseCase {
    private val log = KotlinLogging.logger {}

    // 헤더는 비로그인 요청도 보내므로 무작위 ID 를 뿌리면 집합이 끝없이 커진다. [MAX_SEEN] 을 넘으면 비우고 다시 채운다
    // (비운 뒤에는 이미 기록한 기기도 한 번 더 쓰지만 ON CONFLICT 로 끝난다).
    @Volatile
    private var seen = DaySeen(LocalDate.MIN)

    /**
     * 처음 본 (기기, 로그인 여부, 플랫폼 유무) 만 쓰기를 넘긴다. 비로그인으로 먼저 기록된 기기가 그날 로그인하면 한 번 더 써서 user_id 를 채운다.
     * 쓰기가 실패하면 집합에서 빼 다음 요청에 다시 시도한다.
     */
    override fun record(
        deviceId: String,
        userId: Long?,
        platform: String?,
    ) {
        val today = LocalDate.now(clock.withZone(KST))
        val current = seen.takeIf { it.date == today && it.ids.size < MAX_SEEN } ?: DaySeen(today).also { seen = it }
        // 플랫폼 유무도 키에 넣어, 헤더 없이 먼저 기록된 기기도 첫 유효 플랫폼을 한 번 더 써서 채운다(DB 는 NULL 일 때만 채움).
        val key =
            deviceId + (if (userId == null) "" else LOGGED_IN_SUFFIX) + (if (platform == null) "" else PLATFORM_SUFFIX)
        if (!current.ids.add(key)) return

        try {
            deviceActivityExecutor.execute {
                runCatching { write(deviceId, today, userId, platform) }
                    .onFailure {
                        current.ids.remove(key)
                        // 예외 메시지에는 바인딩 값을 펼친 SQL(기기 ID 포함)이 들어가므로 예외 종류만 남긴다.
                        log.warn { "기기 활동 기록 실패: error=${it.javaClass.name}" }
                    }
            }
        } catch (e: TaskRejectedException) {
            current.ids.remove(key)
            // 폭주 때 요청마다 쌓이지 않게 debug 로 둔다.
            log.debug { "기기 활동 기록 큐가 가득 차 이번 기록을 건너뜀" }
        }
    }

    /**
     * 탈퇴(FOR UPDATE)와 같은 사용자 행을 FOR SHARE 로 잠가 직렬화하고, 활성 사용자일 때만 user_id 를 남긴다.
     * 탈퇴 정리 뒤 늦게 온 쓰기가 user_id 를 되살리지 않게 잠금과 쓰기를 한 트랜잭션으로 묶는다.
     */
    private fun write(
        deviceId: String,
        date: LocalDate,
        userId: Long?,
        platform: String?,
    ) {
        transactionOperations.executeWithoutResult {
            val activeUserId = userId?.takeIf { it in userPersistencePort.lockActiveShared(listOf(it)) }
            devicePersistencePort.recordActivity(deviceId, date, activeUserId, platform)
        }
    }

    private class DaySeen(
        val date: LocalDate,
    ) {
        val ids: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        const val MAX_SEEN = 100_000
        const val LOGGED_IN_SUFFIX = "#user"
        const val PLATFORM_SUFFIX = "#platform"
    }
}
