package com.example.backend.user.application.port.outbound

/** 코스 개수 델타 메시지의 멱등 처리 이력 포트 — 표준 큐(at-least-once) 재전송을 event_id 로 거른다. */
interface ProcessedCourseCountEventPort {
    /** event_id 를 처리 이력에 기록한다(있으면 무시). 새로 기록됐으면 true, 이미 있었으면 false. */
    fun markProcessedIfAbsent(eventId: String): Boolean
}
