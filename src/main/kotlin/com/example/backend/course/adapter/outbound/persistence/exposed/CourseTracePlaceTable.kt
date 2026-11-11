package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseTracePlaceStatus
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

// 따라가기 세션의 장소 — 시작 V2 가 받은 루트는 PLANNED 로 미리 들어가고(order_no = 루트 순서), 체크인하면 VISITED 가 된다.
// 루트 없이 시작한 세션은 체크인할 때 VISITED 행이 생긴다(order_no NULL). 같은 장소가 여러 행일 수 있다.
internal object CourseTracePlaceTable : LongIdTable("tracing_course_places") {
    val tracingCourseId = long("tracing_course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val status = enumerationByName<CourseTracePlaceStatus>("status", 32)
    val orderNo = short("order_no").nullable()
    val visitedAt = timestamp("visited_at").nullable()
}
