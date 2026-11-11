package com.example.backend.course.application.port.outbound

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import java.time.Instant

/** 따라가기 세션 한 행. endedAt 은 COMPLETED 일 때만, 소요 시간·이동 거리는 종료 전엔 0. */
data class CourseTraceRow(
    val id: Long,
    val userId: Long,
    val courseId: Long,
    val status: CourseTraceStatus,
    val startedAt: Instant,
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
)

/** 아웃바운드 포트 — 코스 따라가기 세션(tracing_courses·tracing_course_places) 접근. 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다. */
interface CourseTracePersistencePort {
    /** 사용자의 진행 중 세션(사용자당 최대 하나). */
    fun findInProgress(userId: Long): CourseTraceRow?

    /** 사용자의 완주 세션을 id DESC(최근 시작 순)로 [beforeId] 미만부터 최대 [size] 건 읽는다(hasNext 판별용 +1 은 호출부가 더한다). */
    fun findCompleted(
        userId: Long,
        beforeId: Long?,
        size: Int,
    ): List<CourseTraceRow>

    /** 진행 중 세션을 만들고 id 를 돌려준다. 이미 진행 중 세션이 있으면(동시 시작) null. */
    fun insertInProgress(
        userId: Long,
        courseId: Long,
    ): Long?

    /** 세션이 따라갈 루트(장소 id, 따라갈 순서)를 저장한다. */
    fun insertRoutePlaces(
        traceId: Long,
        placeIds: List<Long>,
    )

    /** 세션 루트의 방문 전 장소를 [placeIds] 로 바꾼다 — 체크인한 장소는 체크인 순으로 앞에 두고 그 뒤에 잇는다. */
    fun replaceRemainingRoutePlaces(
        traceId: Long,
        placeIds: List<Long>,
    )

    /** 세션 루트의 장소 id 를 순서대로(방문 여부 무관). 루트 없이 시작한 세션(코스 장소 그대로 따라가기)은 빈 목록. */
    fun findRoutePlaceIds(traceId: Long): List<Long>

    /** 세션 행을 잠그고 읽는다(FOR UPDATE) — 쓰기 트랜잭션 안에서만 호출한다. */
    fun findById(traceId: Long): CourseTraceRow?

    /** 세션 행을 잠그지 않고 읽는다 — 읽기 트랜잭션(화면 조회)용. */
    fun readById(traceId: Long): CourseTraceRow?

    /** 세션의 체크인 장소를 체크인 시각 순으로. */
    fun findVisitedPlaces(traceId: Long): List<VisitedPlaceResult>

    /** 방문 기록 — 루트에 그 장소의 방문 전 항목이 있으면 가장 앞선 것을 방문 처리하고, 없으면 새로 넣는다. 재전송·방문 횟수 초과는 호출부가 거른다. */
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

    /** 사용자의 따라가기 세션·체크인을 전부 하드 삭제한다(탈퇴 정리). */
    fun deleteAllByUser(userId: Long)
}
