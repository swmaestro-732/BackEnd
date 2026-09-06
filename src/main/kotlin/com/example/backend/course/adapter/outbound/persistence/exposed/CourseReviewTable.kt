package com.example.backend.course.adapter.outbound.persistence.exposed

import com.example.backend.course.domain.model.CourseReviewStatus
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(CourseReviewEntity)·DSL 공용.
internal object CourseReviewTable : LongIdTable("course_reviews") {
    val courseId = long("course_id") // 같은 도메인(course) 참조 — 스키마에 FK 있음
    val status = enumerationByName<CourseReviewStatus>("status", 32)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    val userId = long("user_id") // cross-domain(user): FK 없음
    val rating = short("rating")
    val content = text("content").nullable()
}

/** course_reviews 테이블의 DAO 엔티티([CourseReviewTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class CourseReviewEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CourseReviewEntity>(CourseReviewTable)

    var courseId by CourseReviewTable.courseId
    var status by CourseReviewTable.status
    var createdAt by CourseReviewTable.createdAt
    var updatedAt by CourseReviewTable.updatedAt
    var deletedAt by CourseReviewTable.deletedAt
    var userId by CourseReviewTable.userId
    var rating by CourseReviewTable.rating
    var content by CourseReviewTable.content
}
