package com.example.backend.admin.application.port.outbound

/** 어드민 신원 확인 포트. 구글 ID 토큰을 검증하고 인증된 이메일(소문자)을 돌려준다. */
interface AdminIdentityPort {
    fun verifyGoogle(idToken: String): String
}
