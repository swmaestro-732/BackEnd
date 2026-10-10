package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// 코스 따라가기 기록 — 종료 시 1행 삽입(같은 코스를 여러 번 따라가면 행이 쌓인다). 소요 시간·이동 거리는 클라이언트 측정값.
internal object CourseTraceTable : LongIdTable("tracing_courses") {
    val userId = long("user_id") // cross-domain(user): FK 없음
    val courseId = long("course_id")
    val durationMinutes = integer("duration_minutes")
    val distanceMeters = integer("distance_meters")
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
}
