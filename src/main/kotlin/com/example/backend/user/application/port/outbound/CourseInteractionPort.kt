package com.example.backend.user.application.port.outbound

import java.time.Instant

/**
 * 아웃바운드 포트 — 사용자·코스 상호작용(저장·따라가기) 배치 조회.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseInteractionPort {
    /**
     * 주어진 코스들 중 사용자가 따라간(=완주한) 코스의 완주 시각(가장 이른 tracing 시각)을 courseId 별로 반환한다.
     * 따라가지 않은 코스는 결과에 없다(빈 목록이면 빈 맵).
     */
    fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant>

    /** 주어진 코스들 중 사용자가 저장한 코스의 id 집합을 반환한다(배치 조회자 상태용). */
    fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long>
}
