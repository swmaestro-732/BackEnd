package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.notification.application.port.inbound.NotificationCleanupUseCase
import com.example.backend.user.application.port.outbound.NotificationCleanupPort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [NotificationCleanupPort] 를 notification 인바운드 포트에 위임한다(인프로세스). */
@Component
class NotificationCleanupAdapter(
    private val notificationCleanupUseCase: NotificationCleanupUseCase,
) : NotificationCleanupPort {
    override fun purgeByUser(userId: Long) = notificationCleanupUseCase.purgeByUser(userId)
}
