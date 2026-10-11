package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTracePlaceTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTraceTable
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/** tracing_courses·tracing_course_places 테이블 접근 리포지토리 — 따라가기 기록·방문 장소 삽입과 완주 조회. */
@Repository
class CourseTraceRepository {
    fun insert(command: EndCourseTraceCommand) {
        val tracingCourseId =
            CourseTraceTable
                .insertAndGetId {
                    it[userId] = command.userId
                    it[courseId] = command.courseId
                    it[durationMinutes] = command.durationMinutes
                    it[distanceMeters] = command.distanceMeters
                }.value
        if (command.visitedPlaces.isEmpty()) return
        CourseTracePlaceTable.batchInsert(
            command.visitedPlaces.withIndex(),
            shouldReturnGeneratedValues = false,
        ) { (index, place) ->
            this[CourseTracePlaceTable.tracingCourseId] = tracingCourseId
            this[CourseTracePlaceTable.placeId] = place.placeId
            this[CourseTracePlaceTable.orderNo] = index.toShort()
            this[CourseTracePlaceTable.visitedAt] = place.visitedAt.toKotlinInstant()
        }
    }

    /** (user, courseIds) 중 따라간 코스의 가장 이른 created_at 을 courseId 별로 반환한다(없으면 빈 맵). */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> {
        if (courseIds.isEmpty()) return emptyMap()
        val firstTracedAt = CourseTraceTable.createdAt.min()
        return CourseTraceTable
            .select(CourseTraceTable.courseId, firstTracedAt)
            .where { (CourseTraceTable.userId eq userId) and (CourseTraceTable.courseId inList courseIds) }
            .groupBy(CourseTraceTable.courseId)
            .associate { it[CourseTraceTable.courseId] to it[firstTracedAt]!!.toJavaInstant() }
    }

    /** 사용자가 한 번이라도 따라간 코스 id 집합. */
    fun findTracedCourseIds(userId: Long): Set<Long> =
        CourseTraceTable
            .select(CourseTraceTable.courseId)
            .where { CourseTraceTable.userId eq userId }
            .withDistinct()
            .mapTo(mutableSetOf()) { it[CourseTraceTable.courseId] }
}
