package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [CourseInteractionPort] 를 Exposed 로 구현한다.
 * saved_courses·tracing_courses 에 (user_id, course_id) 행 존재 여부만 확인한다.
 */
@Repository
class CourseInteractionAdapter : CourseInteractionPort {
    override fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        SavedCourseTable
            .selectAll()
            .where { (SavedCourseTable.userId eq userId) and (SavedCourseTable.courseId eq courseId) }
            .empty()
            .not()

    override fun existsTracingCourse(
        userId: Long,
        courseId: Long,
    ): Boolean =
        TracingCourseTable
            .selectAll()
            .where { (TracingCourseTable.userId eq userId) and (TracingCourseTable.courseId eq courseId) }
            .empty()
            .not()
}
