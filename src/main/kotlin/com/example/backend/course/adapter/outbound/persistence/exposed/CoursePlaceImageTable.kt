package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(CoursePlaceImageEntity)·DSL 공용.
internal object CoursePlaceImageTable : LongIdTable("course_place_images") {
    val coursePlaceId = long("course_place_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no").nullable()
}

/** course_place_images 테이블의 DAO 엔티티([CoursePlaceImageTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class CoursePlaceImageEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CoursePlaceImageEntity>(CoursePlaceImageTable)

    var coursePlaceId by CoursePlaceImageTable.coursePlaceId
    var imageUrl by CoursePlaceImageTable.imageUrl
    var orderNo by CoursePlaceImageTable.orderNo
}
