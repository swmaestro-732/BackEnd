package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CoursePlaceImageTable : Table("course_places_images") {
    val id = long("id").autoIncrement()
    val coursePlacesId = long("course_places_id")
    val imageUrl = text("image_url").nullable()
    val orderNo = short("order_no").nullable()
    override val primaryKey = PrimaryKey(id)
}
