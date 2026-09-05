package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(CoursePlaceEntity)·DSL 공용.
internal object CoursePlaceTable : LongIdTable("course_places") {
    val courseId = long("course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val orderNo = short("order_no")
    val caption = varchar("caption", 200).nullable()

    /** 다음 장소까지 도보 소요 시간(분). 마지막 장소는 다음 이동이 없어 nullable. */
    val walkingMinutes = integer("walking_minutes").nullable()
}

/** course_places 테이블의 DAO 엔티티([CoursePlaceTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class CoursePlaceEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CoursePlaceEntity>(CoursePlaceTable)

    var courseId by CoursePlaceTable.courseId
    var placeId by CoursePlaceTable.placeId
    var orderNo by CoursePlaceTable.orderNo
    var caption by CoursePlaceTable.caption
    var walkingMinutes by CoursePlaceTable.walkingMinutes
}
