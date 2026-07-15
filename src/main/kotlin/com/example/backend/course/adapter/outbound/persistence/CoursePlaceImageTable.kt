package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CoursePlaceImageTable : Table("course_place_images") {
    val id = long("id").autoIncrement()
    val coursePlaceId = long("course_place_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no").nullable()
    override val primaryKey = PrimaryKey(id)
}
