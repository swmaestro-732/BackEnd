package com.example.backend.user.application.port.inbound

import com.example.backend.common.domain.CourseVisibility

/**
 * 인바운드 포트 — 코스 공개범위 전이를 작성자 개수 버킷에 멱등하게 반영한다(카운트를 소유한 user 도메인).
 *
 * 동기(in-process ACL 이벤트 핸들러)·폴백(SQS 컨슈머) 두 경로가 **같은** 이 메서드를 호출한다.
 * 델타 계산·멱등(eventId)이 이 한 곳에 모여 있어 SQS at-least-once 재전송에도 이중 집계가 없다.
 * 공개범위는 카운트 대상이 아니면 null(임시저장·삭제 후). [eventId] 는 멱등키.
 */
interface CourseCountUseCase {
    fun apply(
        eventId: String,
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
    )
}
