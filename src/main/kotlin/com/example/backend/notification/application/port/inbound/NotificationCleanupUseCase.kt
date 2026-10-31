package com.example.backend.notification.application.port.inbound

/** 탈퇴 정리 전용 포트. */
interface NotificationCleanupUseCase {
    fun purgeByUser(userId: Long)
}
