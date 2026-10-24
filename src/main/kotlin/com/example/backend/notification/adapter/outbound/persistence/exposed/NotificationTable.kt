package com.example.backend.notification.adapter.outbound.persistence.exposed

import com.example.backend.notification.domain.model.NotificationTargetType
import com.example.backend.notification.domain.model.NotificationType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object NotificationTable : LongIdTable("notifications") {
    val userId = long("user_id")
    val type = enumerationByName<NotificationType>("type", 32)
    val targetType = enumerationByName<NotificationTargetType>("target_type", 32)
    val targetId = long("target_id")
    val windowStart = timestamp("window_start")
    val lastActorId = long("last_actor_id")
    val actorCount = integer("actor_count")

    // JSONB는 조회 시 문자열로 읽고, 쓰기는 업서트 SQL에서 JSONB로 바인딩한다.
    val payload = text("payload")
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
