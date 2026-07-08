package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

// TODO: support 도메인 미도입 상태라 가장 가까운 member 도메인에 임시 배치. 전용 도메인 생성 시 이동.
internal object InquiryTable : Table("inquiries") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").nullable() // cross-domain(member): FK 없음
    val category = varchar("category", 50).nullable()
    val title = varchar("title", 200)
    val content = text("content")
    val status = short("status")
    val answer = text("answer").nullable()
    val answeredAt = timestamp("answered_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
