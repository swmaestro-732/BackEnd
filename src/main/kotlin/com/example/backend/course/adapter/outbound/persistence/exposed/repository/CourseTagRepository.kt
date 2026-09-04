package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTagTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.springframework.stereotype.Repository

/**
 * course_tags 테이블 접근 리포지토리 — 코스와 태그의 연결(조인 테이블) 삽입.
 */
@Repository
class CourseTagRepository {
    /** 코스와 태그들의 연결을 배치 1문으로 삽입한다 — 생성 값이 없는 조인 테이블이라 RETURNING 을 끈다. */
    fun linkAll(
        courseId: Long,
        tagIds: List<Long>,
    ) {
        CourseTagTable.batchInsert(tagIds, shouldReturnGeneratedValues = false) { tagId ->
            this[CourseTagTable.courseId] = courseId
            this[CourseTagTable.tagId] = tagId
        }
    }

    /** 코스의 태그 연결을 모두 삭제한다(전체 치환 편집 전처리). */
    fun deleteByCourseId(courseId: Long) {
        CourseTagTable.deleteWhere { CourseTagTable.courseId eq courseId }
    }
}
