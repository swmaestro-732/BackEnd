package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 접근은 DSL 로만 한다(DAO 엔티티 없음).
internal object CourseReviewPhotoTable : LongIdTable("course_review_photos") {
    val courseReviewId = long("course_review_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no")
}
