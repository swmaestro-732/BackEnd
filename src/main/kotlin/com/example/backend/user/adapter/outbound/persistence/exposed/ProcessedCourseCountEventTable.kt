package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * V5__processed_course_count_events.sql 매핑 — 코스 개수 델타 메시지의 멱등 처리 이력.
 * PK 가 event_id(대리키 아님)라 LongIdTable 이 아닌 [Table] 로 정의한다.
 * 표준 큐(at-least-once) 재전송을 event_id 유니크로 걸러 델타를 한 번만 반영한다.
 */
internal object ProcessedCourseCountEventTable : Table("processed_course_count_events") {
    val eventId = varchar("event_id", 64)

    // DB DEFAULT now() 가 채운다(Flyway 소유) → insert 에서 생략하도록 databaseGenerated 로 표시.
    val processedAt = timestamp("processed_at").databaseGenerated()
    override val primaryKey = PrimaryKey(eventId)
}
