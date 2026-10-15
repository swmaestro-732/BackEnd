package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseTraceStatus
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// 코스 따라가기 세션 — 시작 시 IN_PROGRESS 로 1행, 종료 시 COMPLETED. created_at 이 시작 시각. 소요 시간·이동 거리는 클라이언트 측정값.
object CourseTraceTable : LongIdTable("tracing_courses") {
    val userId = long("user_id") // cross-domain(user): FK 없음
    val courseId = long("course_id")
    val status = enumerationByName<CourseTraceStatus>("status", 32)
    val durationMinutes = integer("duration_minutes")
    val distanceMeters = integer("distance_meters")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val endedAt = timestamp("ended_at").nullable()
}
