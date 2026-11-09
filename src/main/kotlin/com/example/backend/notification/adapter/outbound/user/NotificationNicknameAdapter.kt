package com.example.backend.notification.adapter.outbound.user

import com.example.backend.notification.application.port.outbound.NotificationNicknamePort
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Component

@Component
class NotificationNicknameAdapter(
    private val userSummaryUseCase: UserSummaryUseCase,
) : NotificationNicknamePort {
    override fun findNicknames(userIds: Collection<Long>): Map<Long, String> =
        userSummaryUseCase.findSummaries(userIds).associate { it.id to it.nickname }
}
