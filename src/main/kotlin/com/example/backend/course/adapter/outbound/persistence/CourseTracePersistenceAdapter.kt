package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTraceRepository
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import org.springframework.stereotype.Component
import java.time.Instant

/** 아웃바운드 어댑터 — [CourseTracePersistencePort] 를 구현한다. 테이블 접근은 [CourseTraceRepository] 에 위임한다. */
@Component
class CourseTracePersistenceAdapter(
    private val courseTraceRepository: CourseTraceRepository,
) : CourseTracePersistencePort {
    override fun findInProgress(
        userId: Long,
        courseId: Long,
    ): CourseTraceRow? = courseTraceRepository.findInProgress(userId, courseId)

    override fun insertInProgress(
        userId: Long,
        courseId: Long,
    ): Long? = courseTraceRepository.insertInProgress(userId, courseId)

    override fun findById(traceId: Long): CourseTraceRow? = courseTraceRepository.findById(traceId)

    override fun findVisitedPlaces(traceId: Long): List<VisitedPlaceResult> =
        courseTraceRepository.findVisitedPlaces(traceId)

    override fun insertVisitedPlace(
        traceId: Long,
        placeId: Long,
        visitedAt: Instant,
    ) = courseTraceRepository.insertVisitedPlace(traceId, placeId, visitedAt)

    override fun complete(
        traceId: Long,
        durationMinutes: Int,
        distanceMeters: Int,
    ): Int = courseTraceRepository.complete(traceId, durationMinutes, distanceMeters)

    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = courseTraceRepository.findCompletedAt(userId, courseIds)

    override fun findTracedCourseIds(userId: Long): Set<Long> = courseTraceRepository.findTracedCourseIds(userId)

    override fun deleteAllByUser(userId: Long) = courseTraceRepository.deleteAllByUser(userId)
}
