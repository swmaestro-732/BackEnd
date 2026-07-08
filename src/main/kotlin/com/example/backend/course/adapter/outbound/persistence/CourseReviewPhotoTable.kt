package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

internal object CourseReviewPhotoTable : Table("course_review_photos") {
    val id = long("id").autoIncrement()
    val courseReviewId = long("course_review_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no")
    override val primaryKey = PrimaryKey(id)
}
