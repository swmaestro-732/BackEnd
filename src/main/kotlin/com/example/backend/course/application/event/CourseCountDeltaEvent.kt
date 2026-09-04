package com.example.backend.course.application.event

/**
 * 도메인 이벤트 — 코스 상태 변화로 작성자의 공개범위별 코스 개수가 바뀌었음을 알린다.
 * 커밋 후(AFTER_COMMIT) 리스너가 SQS 로 흘려보내고, user 도메인 컨슈머가 결과적 일관성으로 카운터를 반영한다.
 *
 * 세 버킷의 델타만 담아(크로스 도메인 경계라 공개범위 enum 을 넘기지 않는다) 표준 큐(at-least-once)로 보낸다.
 * [eventId] 는 컨슈머 멱등키 — 재전송돼도 한 번만 반영되도록 처리 이력으로 중복을 거른다.
 */
data class CourseCountDeltaEvent(
    val userId: Long,
    val publicDelta: Int,
    val followerDelta: Int,
    val privateDelta: Int,
    val eventId: String,
)
