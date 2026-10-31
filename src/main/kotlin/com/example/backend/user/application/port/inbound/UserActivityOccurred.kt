package com.example.backend.user.application.port.inbound

import java.time.Instant

/** 다른 도메인이 구독하는 활동 이벤트 공개 계약. */
interface UserActivityOccurred {
    val kind: UserActivityKind
    val actorId: Long
    val targetId: Long
    val occurredAt: Instant
}
