package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseTable
import com.example.backend.user.adapter.outbound.persistence.exposed.TracingCourseTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
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

    /**
     * 코스별 저장수(saved_courses 행 수)를 courseId → count 로 집계한다(GROUP BY course_id).
     * 저장 기록이 없는 courseId 는 결과 맵에서 빠진다(호출측이 0 으로 처리). 빈 입력이면 조회 없이 빈 맵.
     */
    fun countSavesByCourseIds(courseIds: List<Long>): Map<Long, Int> {
        if (courseIds.isEmpty()) return emptyMap()
        val cnt = SavedCourseTable.courseId.count()
        return SavedCourseTable
            .select(SavedCourseTable.courseId, cnt)
            .where { SavedCourseTable.courseId inList courseIds }
            .groupBy(SavedCourseTable.courseId)
            .associate { it[SavedCourseTable.courseId] to it[cnt].toInt() }
    }
}
