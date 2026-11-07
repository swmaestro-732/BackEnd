package com.example.backend.admin.application.port.inbound

import com.example.backend.admin.application.port.inbound.dto.AdminLogin

/** 어드민 로그인 인바운드 포트. 구글 ID 토큰만 받는다. */
interface AdminAuthUseCase {
    fun login(idToken: String): AdminLogin
}
