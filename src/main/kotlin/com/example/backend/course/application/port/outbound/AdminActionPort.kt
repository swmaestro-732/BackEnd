package com.example.backend.course.application.port.outbound

/** 아웃바운드 포트: 어드민 코스 조치 감사 기록(admin 도메인 admin_actions). 조치와 같은 트랜잭션에 남는다. */
interface AdminActionPort {
    fun record(
        adminEmail: String,
        courseId: Long,
        action: String,
    )
}
