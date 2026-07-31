package com.example.backend.course.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(CourseReviewTagEntity)·DSL 공용.
internal object CourseReviewTagTable : LongIdTable("course_review_tags") {
    val label = varchar("label", 20)
    val icon = varchar("icon", 20)
}

/** course_review_tags 테이블의 DAO 엔티티([CourseReviewTagTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class CourseReviewTagEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<CourseReviewTagEntity>(CourseReviewTagTable)

    var label by CourseReviewTagTable.label
    var icon by CourseReviewTagTable.icon
}
