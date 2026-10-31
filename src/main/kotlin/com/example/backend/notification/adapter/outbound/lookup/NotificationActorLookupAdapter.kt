package com.example.backend.notification.adapter.outbound.lookup

import com.example.backend.notification.application.port.outbound.NotificationActor
import com.example.backend.notification.application.port.outbound.NotificationActorLookupPort
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Component

@Component
class NotificationActorLookupAdapter(
    private val users: UserUseCase,
) : NotificationActorLookupPort {
    override fun findActor(actorId: Long): NotificationActor? =
        users
            .getProfiles(
                listOf(actorId),
                null,
            ).firstOrNull()
            ?.let { NotificationActor(it.nickname, it.profileImageUrl) }
}
