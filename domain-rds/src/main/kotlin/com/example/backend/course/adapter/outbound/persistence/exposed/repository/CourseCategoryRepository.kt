package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseCategoryTable
import com.example.backend.course.domain.model.CourseCategory
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/** course_categories 테이블 접근 리포지토리 — 코스에 고른 카테고리 연결. */
@Repository
class CourseCategoryRepository {
    fun insertAll(
        courseId: Long,
        categories: List<CourseCategory>,
    ) {
        CourseCategoryTable.batchInsert(categories, shouldReturnGeneratedValues = false) { category ->
            this[CourseCategoryTable.courseId] = courseId
            this[CourseCategoryTable.category] = category
        }
    }

    /** 코스의 카테고리 연결을 모두 삭제한다(전체 치환 편집 전처리). */
    fun deleteByCourseId(courseId: Long) {
        CourseCategoryTable.deleteWhere { CourseCategoryTable.courseId eq courseId }
    }

    /** 여러 코스의 카테고리를 한 번에 읽는다(코스 id → enum 선언 순 카테고리). 목록 조회의 N+1 을 피한다. */
    fun findByCourseIds(courseIds: Collection<Long>): Map<Long, List<CourseCategory>> {
        if (courseIds.isEmpty()) return emptyMap()
        return CourseCategoryTable
            .select(CourseCategoryTable.courseId, CourseCategoryTable.category)
            .where { CourseCategoryTable.courseId inList courseIds }
            .map { it[CourseCategoryTable.courseId] to it[CourseCategoryTable.category] }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, categories) -> categories.sortedBy { it.ordinal } }
    }
}
