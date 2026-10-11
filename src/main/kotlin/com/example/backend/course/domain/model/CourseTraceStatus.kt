package com.example.backend.course.domain.model

/** 따라가기 세션 상태 — 시작하면 IN_PROGRESS, 종료하면 COMPLETED. ABANDONED 는 오래된 진행 중 세션 정리용으로 예약. */
enum class CourseTraceStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}
