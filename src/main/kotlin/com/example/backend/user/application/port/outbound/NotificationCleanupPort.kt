package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 탈퇴 정리 중 알림 도메인 데이터를 지운다.
 * user 애플리케이션이 notification 을 직접 알지 않게 하는 경계([CourseCleanupPort] 와 같은 방식).
 */
interface NotificationCleanupPort {
    /** 탈퇴자가 받은 알림, 탈퇴자가 마지막 행위자인 알림, 수신 설정을 지운다. */
    fun purgeByUser(userId: Long)
}
