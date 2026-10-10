package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

// 따라가기 중 방문(체크)한 장소 — 요청 순서(order_no)와 도착 시각(visited_at).
internal object CourseTracePlaceTable : LongIdTable("tracing_course_places") {
    val tracingCourseId = long("tracing_course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val orderNo = short("order_no")
    val visitedAt = timestamp("visited_at")
}
