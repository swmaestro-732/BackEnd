package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CoursePlaceTable : Table("course_places") {
    val id = long("id").autoIncrement()
    val courseId = long("course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val orderNo = short("order_no")
    val caption = varchar("caption", 200).nullable()

    /** 다음 장소까지 도보 소요 시간(분). 마지막 장소는 다음 이동이 없어 nullable. */
    val walkingMinutes = integer("walking_minutes").nullable()
    override val primaryKey = PrimaryKey(id)
}
