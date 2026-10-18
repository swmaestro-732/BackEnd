package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

// 따라가기 중 방문(체크)한 장소 — 체크인 순서는 id 순, (tracing_course_id, place_id) 유니크.
internal object CourseTracePlaceTable : LongIdTable("tracing_course_places") {
    val tracingCourseId = long("tracing_course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val visitedAt = timestamp("visited_at")
}
