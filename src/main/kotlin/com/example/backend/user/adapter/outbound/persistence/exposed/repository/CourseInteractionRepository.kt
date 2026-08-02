package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseTable
import com.example.backend.user.adapter.outbound.persistence.exposed.TracingCourseTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * saved_courses·tracing_courses 테이블 접근 리포지토리.
 * (user_id, course_id) 행 존재 여부만 확인한다.
 */
@Repository
class CourseInteractionRepository {
    fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable
            .selectAll()
            .where { (SavedCourseTable.userId eq userId) and (SavedCourseTable.courseId eq courseId) }
            .empty()
            .not()

    fun existsTracingCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        TracingCourseTable
            .selectAll()
            .where { (TracingCourseTable.userId eq userId) and (TracingCourseTable.courseId eq courseId) }
            .empty()
            .not()
}
