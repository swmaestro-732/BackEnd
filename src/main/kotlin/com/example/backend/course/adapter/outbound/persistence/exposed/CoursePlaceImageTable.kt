package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 삽입은 batchInsert(DSL), 조회도 DSL 로만 접근한다.
internal object CoursePlaceImageTable : LongIdTable("course_place_images") {
    val coursePlaceId = long("course_place_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no").nullable()
}
