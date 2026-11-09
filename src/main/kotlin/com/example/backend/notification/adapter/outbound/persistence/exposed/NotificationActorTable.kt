package com.example.backend.notification.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table

internal object NotificationActorTable : Table("notification_actors") {
    val notificationId = long("notification_id")
    val actorId = long("actor_id")
    override val primaryKey = PrimaryKey(notificationId, actorId)
}
