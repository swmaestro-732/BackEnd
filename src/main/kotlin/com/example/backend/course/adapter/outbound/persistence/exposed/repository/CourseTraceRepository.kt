package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTracePlaceTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTraceTable
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.springframework.stereotype.Repository
import kotlin.time.toKotlinInstant

/** tracing_courses·tracing_course_places 테이블 접근 리포지토리 — 따라가기 기록과 방문 장소 삽입. */
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
}
