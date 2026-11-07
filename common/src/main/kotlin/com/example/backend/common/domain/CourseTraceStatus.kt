package com.example.backend.common.domain

/**
 * 따라가기 세션 상태 — 시작하면 IN_PROGRESS, 종료하면 COMPLETED. ABANDONED 는 운영 정리(V20 다중 진행 세션 정리)로만 생긴다.
 * course 도메인 소유지만 BFF 응답이 그대로 쓰므로 [CourseVisibility] 처럼 common 에 둔다.
 */
enum class CourseTraceStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}
