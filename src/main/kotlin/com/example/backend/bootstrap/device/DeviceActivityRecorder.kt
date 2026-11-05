package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.config.AsyncConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * 기기의 그날(KST) 활동을 한 번만 기록한다. 이미 기록한 (기기, 날짜) 는 메모리에서 걸러 요청마다 DB 에 쓰지 않는다.
 * 쓰기는 전용 실행기에서 해 요청 스레드가 DB 커넥션을 기다리지 않는다.
 * 인스턴스가 여러 대면 인스턴스마다 한 번씩 쓰지만 PK 가 중복을 막는다.
 */
@Component
class DeviceActivityRecorder(
    private val clock: Clock,
    @Qualifier(AsyncConfig.DEVICE_ACTIVITY_EXECUTOR) private val deviceActivityExecutor: TaskExecutor,
) {
    private val log = KotlinLogging.logger {}

    // 헤더는 비로그인 요청도 보내므로 무작위 ID 를 뿌리면 집합이 끝없이 커진다. [MAX_SEEN] 을 넘으면 비우고 다시 채운다
    // (비운 뒤에는 이미 기록한 기기도 한 번 더 insert 하지만 ON CONFLICT 로 끝난다).
    @Volatile
    private var seen = DaySeen(LocalDate.MIN)

    /** 처음 본 기기만 쓰기를 넘긴다. 쓰기가 실패하면 집합에서 빼 다음 요청에 다시 시도한다. */
    fun record(
        deviceId: String,
        userId: Long?,
    ) {
        val today = LocalDate.now(clock.withZone(KST))
        val current = seen.takeIf { it.date == today && it.ids.size < MAX_SEEN } ?: DaySeen(today).also { seen = it }
        if (!current.ids.add(deviceId)) return

        try {
            deviceActivityExecutor.execute { insert(deviceId, userId, today, current) }
        } catch (e: TaskRejectedException) {
            current.ids.remove(deviceId)
            log.warn { "기기 활동 기록 큐가 가득 차 이번 기록을 건너뜀" }
        }
    }

    private fun insert(
        deviceId: String,
        userId: Long?,
        today: LocalDate,
        current: DaySeen,
    ) {
        runCatching {
            transaction {
                DeviceActivityTable.insertIgnore {
                    it[DeviceActivityTable.deviceId] = deviceId
                    it[activeDate] = today.toKotlinLocalDate()
                    it[DeviceActivityTable.userId] = userId
                }
            }
        }.onFailure {
            current.ids.remove(deviceId)
            log.warn(it) { "기기 활동 기록 실패" }
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
    }
}
