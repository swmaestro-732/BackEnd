package com.example.backend.user.application.port.inbound

/**
 * 인바운드 포트 — 코스 개수 델타 메시지를 멱등하게 반영한다. SQS 폴러(인바운드 어댑터)가 호출한다.
 * [eventId] 는 멱등키로, 재전송된 메시지는 한 번만 반영된다.
 */
interface CourseCountMessageUseCase {
    fun handle(
        eventId: String,
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    )
}
