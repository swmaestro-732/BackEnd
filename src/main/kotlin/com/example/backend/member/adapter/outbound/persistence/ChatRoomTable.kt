package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

// TODO: chat 도메인 미도입 상태라 가장 가까운 member 도메인에 임시 배치. 전용 도메인 생성 시 이동.
internal object ChatRoomTable : Table("chat_rooms") {
    val id = long("id").autoIncrement()
    val h3Idx = varchar("h3_idx", 20)
    val name = varchar("name", 200).nullable()
    val activeUsersCnt = integer("active_users_cnt")
    override val primaryKey = PrimaryKey(id)
}
