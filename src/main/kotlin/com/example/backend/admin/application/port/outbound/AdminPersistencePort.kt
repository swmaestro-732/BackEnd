package com.example.backend.admin.application.port.outbound

interface AdminPersistencePort {
    /** [email] 과 구글 계정 [googleSub] 가 함께 admins 에 등록돼 있는지. */
    fun isRegistered(
        email: String,
        googleSub: String,
    ): Boolean
}
