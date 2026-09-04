package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(CourseReviewPhotoEntity)·DSL 공용.
internal object CourseReviewPhotoTable : LongIdTable("course_review_photos") {
    val courseReviewId = long("course_review_id")
    val imageUrl = text("image_url")
    val orderNo = short("order_no")
}

/** course_review_photos 테이블의 DAO 엔티티([CourseReviewPhotoTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class CourseReviewPhotoEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CourseReviewPhotoEntity>(CourseReviewPhotoTable)

    var courseReviewId by CourseReviewPhotoTable.courseReviewId
    var imageUrl by CourseReviewPhotoTable.imageUrl
    var orderNo by CourseReviewPhotoTable.orderNo
}
