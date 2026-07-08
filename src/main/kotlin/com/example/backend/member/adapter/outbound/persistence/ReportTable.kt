package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

// TODO: support 도메인 미도입 상태라 가장 가까운 member 도메인에 임시 배치. 전용 도메인 생성 시 이동.
internal object ReportTable : Table("reports") {
    val id = long("id").autoIncrement()
    val targetType = short("target_type")
    val targetId = long("target_id") // cross-domain 대상: FK 없음
    val description = text("description")
    val status = short("status")
    val processedAt = timestamp("processed_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
