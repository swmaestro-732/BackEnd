package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 접근은 DSL 로만 한다(DAO 엔티티 없음).
// created_at·updated_at 은 삽입 시 리포지토리가 값을 명시한다(반환 객체 조립용) — clientDefault 는 그 외 경로의 안전망.
internal object PlanTable : LongIdTable("plans") {
    val userId = long("user_id") // cross-domain(user): FK 없음
    val title = varchar("title", 200)
    val memo = text("memo").nullable()
    val plannedDate = date("planned_date").nullable()
    val sourceCourseId = long("source_course_id").nullable() // 계획 복제 원본 course (같은 도메인)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
    val deletedAt = timestamp("deleted_at").nullable()
}
