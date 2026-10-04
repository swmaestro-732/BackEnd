package com.example.backend.user.adapter.messaging

/**
 * 코스 개수 폴백 큐(SQS) 와이어 메시지 — 발행(outbound)·수신(inbound) 어댑터가 공유하는 직렬화 계약.
 * 중립 위치(adapter.messaging)에 둔다. 어느 쪽 어댑터에도 속하지 않아 inbound↔outbound 결합을 만들지 않는다.
 *
 * 전용 카운트 큐라 액션 종류 구분 없이 공개범위 전이(old→new)만 실으면 충분하다 — 처리(델타·멱등)는 eventType 이 아니라
 * 이 필드들로만 이뤄진다. 공개범위는 문자열(enum 이름)로 실어 course 의 enum 에 의존하지 않는다.
 */
data class CourseCountMessage(
    val authorId: Long,
    val oldVisibility: String?,
    val newVisibility: String?,
    val eventId: String,
)
