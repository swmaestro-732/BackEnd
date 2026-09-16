package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 삽입은 batchInsert(DSL), 조회도 DSL 로만 접근한다.
internal object CoursePlaceTable : LongIdTable("course_places") {
    val courseId = long("course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val orderNo = short("order_no")
    val caption = varchar("caption", 200).nullable()

    /** 다음 장소까지 도보 소요 시간(분). 마지막 장소는 다음 이동이 없어 nullable. */
    val walkingMinutes = integer("walking_minutes").nullable()
}
