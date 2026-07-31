package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 사용자·코스 상호작용 존재 여부 조회.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface CourseInteractionPort {
    fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean

    fun existsTracingCourse(
        userId: Long,
        courseId: Long,
    ): Boolean
}
