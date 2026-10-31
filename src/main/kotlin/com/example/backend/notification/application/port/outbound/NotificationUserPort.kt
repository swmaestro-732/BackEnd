package com.example.backend.notification.application.port.outbound

interface NotificationUserPort {
    /** 활성 사용자 행을 잠근다. 탈퇴했으면 USER_NOT_FOUND. */
    fun lockActive(userId: Long)
}
