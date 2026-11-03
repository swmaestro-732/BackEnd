package com.example.backend.admin.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank

/** 어드민 로그인 요청 DTO. 구글 ID 토큰만 받는다. */
data class AdminLoginRequest(
    @field:NotBlank
    val idToken: String,
) {
    override fun toString(): String = "AdminLoginRequest(idToken=[REDACTED])"
}
