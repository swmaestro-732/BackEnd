package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

// TODO: chat 도메인 미도입 상태라 가장 가까운 member 도메인에 임시 배치. 전용 도메인 생성 시 이동.
internal object ChatMessageTable : Table("chat_messages") {
    val id = long("id").autoIncrement()
    val chatRoomId = long("chat_room_id")
    val userId = long("user_id") // cross-domain(member): FK 없음
    val ageGroup = short("age_group").nullable()
    val messageType = short("message_type")
    val content = text("content").nullable()
    val media = text("media").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
