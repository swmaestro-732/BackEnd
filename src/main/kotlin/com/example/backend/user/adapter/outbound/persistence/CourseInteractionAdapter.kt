package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant

/**
 * 아웃바운드 어댑터 — [CourseInteractionPort] 를 Exposed 로 구현한다.
 * saved_courses·tracing_courses 를 (user_id, course_id) 기준으로 배치 조회한다.
 */
@Repository
class CourseInteractionAdapter : CourseInteractionPort {
    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> {
        if (courseIds.isEmpty()) return emptyMap()
        return TracingCourseTable
            .select(TracingCourseTable.courseId, TracingCourseTable.createdAt)
            .where { (TracingCourseTable.userId eq userId) and (TracingCourseTable.courseId inList courseIds) }
            .map { it[TracingCourseTable.courseId] to it[TracingCourseTable.createdAt].toJavaInstant() }
            // 같은 코스를 여러 번 따라간 경우 가장 이른 시각을 완주 시각으로 본다.
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, times) -> times.minOrNull()!! }
    }

    override fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> {
        if (courseIds.isEmpty()) return emptySet()
        return SavedCourseTable
            .select(SavedCourseTable.courseId)
            .where { (SavedCourseTable.userId eq userId) and (SavedCourseTable.courseId inList courseIds) }
            .mapTo(mutableSetOf()) { it[SavedCourseTable.courseId] }
    }
}
