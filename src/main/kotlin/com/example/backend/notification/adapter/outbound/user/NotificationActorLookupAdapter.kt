package com.example.backend.notification.adapter.outbound.user

import com.example.backend.notification.application.port.outbound.NotificationActorLookupPort
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Component

@Component
class NotificationActorLookupAdapter(
    private val userUseCase: UserUseCase,
    private val userSummaryUseCase: UserSummaryUseCase,
) : NotificationActorLookupPort {
    override fun lockActive(userIds: List<Long>): Set<Long> = userUseCase.lockActiveShared(userIds)

    override fun findNickname(actorId: Long): String? =
        userSummaryUseCase.findSummaries(listOf(actorId)).firstOrNull()?.nickname
}
