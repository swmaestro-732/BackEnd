package com.example.backend.user.application.port.outbound

import com.example.backend.common.domain.CourseVisibility

/**
 * 아웃바운드 포트 — 동기 카운트 반영이 실패했을 때 공개범위 전이를 폴백 큐(SQS)로 흘려보낸다.
 * 주 경로는 동기(in-process)이고 이 포트는 실패 시 재시도 경로다(SQS 컨슈머가 같은 use case 로 반영).
 * 구현(어댑터)은 큐 미설정(로컬·CI)·전송 실패를 fail-soft 로 삼킨다.
 */
interface CourseCountFallbackPort {
    fun publish(
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
        eventId: String,
    )
}
