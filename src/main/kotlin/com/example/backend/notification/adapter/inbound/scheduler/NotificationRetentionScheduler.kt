package com.example.backend.notification.adapter.inbound.scheduler

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 매일 새벽 보관 기간이 지난 알림을 지운다. 인스턴스마다 돌지만 같은 조건의 DELETE 라 겹쳐도 결과가 같아 분산 잠금을 두지 않는다.
 */
@Component
class NotificationRetentionScheduler(
    private val notificationCleanupUseCase: NotificationCleanupUseCase,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun purgeExpired() {
        val deleted = notificationCleanupUseCase.purgeExpired()
        log.info { "보관 기간 지난 알림 정리: deleted=$deleted" }
    }
}
