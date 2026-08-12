package com.example.backend.user.application.port.outbound

import java.time.Instant

/**
 * 아웃바운드 포트 — 따라가기(tracing_courses)·체크인(added_places) 접근.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface TracingPersistencePort {
    /** (user, course) 로 진행중(completed_at IS NULL)인 tracing id 를 반환한다 — 중복 시작 차단(409)용. 없으면 null. */
    fun findActiveByUserCourse(
        userId: Long,
        courseId: Long,
    ): Long?

    /** 따라가기 레코드를 삽입하고 생성된 tracing id 를 반환한다. */
    fun insertTracing(
        userId: Long,
        courseId: Long,
    ): Long

    /** 사용자가 소유한 tracing 행(courseId·completedAt)을 반환한다 — 소유가 아니거나 없으면 null. */
    fun findOwned(
        userId: Long,
        tracingId: Long,
    ): TracingRow?

    /** tracing 에 장소 체크인을 삽입한다. (tracing, place) 중복이면 유니크 제약으로 무시한다(멱등). */
    fun checkInPlace(
        tracingId: Long,
        placeId: Long,
    )

    /** tracing 에 체크인된 서로 다른 place 수를 센다. */
    fun countCheckedPlaces(tracingId: Long): Int

    /** tracing 을 완주 처리한다(completed_at 세팅). */
    fun markCompleted(
        tracingId: Long,
        at: Instant,
    )
}

/** 따라가기 행 읽기 모델 — 조회 전용. */
data class TracingRow(
    val id: Long,
    val courseId: Long,
    val completedAt: Instant?,
)
