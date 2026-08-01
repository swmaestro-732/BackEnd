package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.TracingCourseTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant

/** tracing_courses 테이블 접근 리포지토리 — (user_id, course_id) 기준 완주 시각 배치 조회를 담당한다. */
@Repository
class TracingCourseRepository {
    fun findCompletedAt(
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
}
