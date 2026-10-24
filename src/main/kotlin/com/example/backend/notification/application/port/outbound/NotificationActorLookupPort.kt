package com.example.backend.notification.application.port.outbound

data class NotificationActor(
    val nickname: String,
    val profileImageUrl: String?,
)

interface NotificationActorLookupPort {
    fun findActor(actorId: Long): NotificationActor?
}
