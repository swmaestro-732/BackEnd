package com.example.backend.user.application.port.outbound

/** 회원 탈퇴 시 알림 데이터를 정리한다. */
interface NotificationCleanupPort {
    fun purgeByUser(userId: Long)
}
