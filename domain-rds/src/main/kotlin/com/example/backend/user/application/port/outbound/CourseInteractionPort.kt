package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 사용자·코스 상호작용(저장) 배치 조회.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseInteractionPort {
    /** 주어진 코스들 중 사용자가 저장한 코스의 id 집합을 반환한다(배치 조회자 상태용). */
    fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long>
}
