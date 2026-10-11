package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.course.domain.model.CourseTraceStatus
import java.time.Instant

/** 따라가기 세션 한 행. */
data class CourseTraceRow(
    val id: Long,
    val userId: Long,
    val courseId: Long,
    val status: CourseTraceStatus,
    val startedAt: Instant,
)

/** 아웃바운드 포트 — 코스 따라가기 세션(tracing_courses·tracing_course_places) 접근. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다. */
interface CourseTracePersistencePort {
    /** (user, course) 의 진행 중 세션. */
    fun findInProgress(
        userId: Long,
        courseId: Long,
    ): CourseTraceRow?

    /** 진행 중 세션을 만들고 id 를 돌려준다. 이미 진행 중 세션이 있으면(동시 시작) null. */
    fun insertInProgress(
        userId: Long,
        courseId: Long,
    ): Long?

    fun findById(traceId: Long): CourseTraceRow?

    /** 세션의 체크인 장소를 체크인 순서대로. */
    fun findVisitedPlaces(traceId: Long): List<VisitedPlaceResult>

    /** 장소 체크인 — 같은 장소가 이미 있으면 무시한다. */
    fun insertVisitedPlace(
        traceId: Long,
        placeId: Long,
        visitedAt: Instant,
    )

    /** 진행 중 세션을 COMPLETED 로 바꾸고 소요 시간·이동 거리·종료 시각을 기록한다. 반환은 갱신 행 수(이미 종료됐으면 0). */
    fun complete(
        traceId: Long,
        durationMinutes: Int,
        distanceMeters: Int,
    ): Int

    /** 주어진 코스들 중 사용자가 완주한 코스의 가장 이른 종료 시각을 courseId 별로 반환한다. 완주하지 않은 코스는 없다. */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 사용자가 한 번이라도 완주한 코스 id 집합. */
    fun findTracedCourseIds(userId: Long): Set<Long>
}
