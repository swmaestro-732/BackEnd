package com.example.backend.course.application.port.inbound

import java.time.Instant

/** 다른 도메인이 구독하는 활동 이벤트 공개 계약. */
interface CourseActivityOccurred {
    val kind: CourseActivityKind
    val actorId: Long
    val courseId: Long
    val occurredAt: Instant
}
