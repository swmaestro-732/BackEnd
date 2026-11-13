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

/**
 * 리텐션 측정용으로 기기의 그날(KST) 활동을 `device_daily_activity` 에 남긴다(SCRUM-783).
 *
 * 요청 스레드는 [RecordedDeviceActivities] 로 "오늘 처음 보는 활동"만 골라 전용 실행기에 넘기고 바로 돌아간다(DB 를 기다리지 않는다).
 * 실행기 스레드는 사용자 잠금과 upsert 를 한 트랜잭션으로 쓴다([write]). 큐가 차거나 쓰기가 실패하면 기억을 지워 다음 요청에 다시 시도한다.
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
    private val recordedActivities = RecordedDeviceActivities()

    override fun record(
        deviceId: String,
        userId: Long?,
        platform: String?,
    ) {
        val activity = DeviceActivity(deviceId, LocalDate.now(clock.withZone(KST)), userId, platform)
        if (recordedActivities.markNew(activity)) submit(activity)
    }

    private fun submit(activity: DeviceActivity) {
        try {
            deviceActivityExecutor.execute { writeOrForget(activity) }
        } catch (e: TaskRejectedException) {
            recordedActivities.forget(activity)
            // 폭주 때 요청마다 쌓이지 않게 debug 로 둔다.
            log.debug { "기기 활동 기록 큐가 가득 차 이번 기록을 건너뜀" }
        }
    }

    private fun writeOrForget(activity: DeviceActivity) {
        try {
            write(activity)
        } catch (e: Exception) {
            recordedActivities.forget(activity)
            // 예외 메시지에는 바인딩 값을 펼친 SQL(기기 ID 포함)이 들어가므로 예외 종류만 남긴다.
            log.warn { "기기 활동 기록 실패: error=${e.javaClass.name}" }
        }
    }

    /**
     * 탈퇴(FOR UPDATE)와 같은 사용자 행을 FOR SHARE 로 잠가 줄을 세우고, 활성 사용자일 때만 user_id 를 남긴다.
     * 잠금은 트랜잭션이 끝나면 풀리므로 잠금과 upsert 를 한 트랜잭션에 묶어야, 탈퇴 정리 뒤 늦게 온 쓰기가 user_id 를 되살리지 않는다.
     * 실행기 스레드에서 부르는 같은 클래스 메서드라 @Transactional 프록시가 걸리지 않아 [TransactionOperations] 로 연다.
     */
    private fun write(activity: DeviceActivity) {
        transactionOperations.executeWithoutResult {
            val activeUserId = activity.userId?.takeIf { it in userPersistencePort.lockActiveShared(listOf(it)) }
            devicePersistencePort.recordActivity(activity.deviceId, activity.date, activeUserId, activity.platform)
        }
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
