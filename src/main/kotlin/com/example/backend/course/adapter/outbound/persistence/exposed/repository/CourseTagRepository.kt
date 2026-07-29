package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.CourseTagTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.springframework.stereotype.Repository

/**
 * course_tags 테이블 접근 리포지토리 — 코스와 태그의 연결(조인 테이블) 삽입.
 */
@Repository
class CourseTagRepository {
    fun link(
        courseId: Long,
        tagId: Long,
    ) {
        CourseTagTable.insert {
            it[CourseTagTable.courseId] = courseId
            it[CourseTagTable.tagId] = tagId
        }
    }

    /** 코스의 태그 연결을 모두 삭제한다(전체 치환 편집 전처리). */
    fun deleteByCourseId(courseId: Long) {
        CourseTagTable.deleteWhere { CourseTagTable.courseId eq courseId }
    }
}
