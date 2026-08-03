package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.SavedCourseTable
import com.example.backend.user.adapter.outbound.persistence.exposed.TracingCourseTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant

/**
 * saved_courses·tracing_courses 테이블 접근 리포지토리 — 조회자 상태(저장 여부·완주 시각) 배치 조회를 담당한다.
 */
@Repository
class CourseInteractionRepository {
    /** 주어진 코스들 중 사용자가 따라간(완주한) 코스의 가장 이른 tracing 시각을 courseId 별로 반환한다(없으면 빈 맵). */
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

    /** 주어진 코스들 중 사용자가 저장(deleted_at IS NULL)한 코스의 id 집합을 반환한다. */
    fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> {
        if (courseIds.isEmpty()) return emptySet()
        return SavedCourseTable
            .select(SavedCourseTable.courseId)
            .where {
                (SavedCourseTable.userId eq userId) and
                    (SavedCourseTable.courseId inList courseIds) and
                    SavedCourseTable.deletedAt.isNull()
            }.mapTo(mutableSetOf()) { it[SavedCourseTable.courseId] }
    }
}
