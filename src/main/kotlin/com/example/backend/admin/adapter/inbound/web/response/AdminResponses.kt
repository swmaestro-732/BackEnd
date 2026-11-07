package com.example.backend.admin.adapter.inbound.web.response

import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import java.time.Instant

/** 어드민 로그인 응답. */
data class AdminLoginResponse(
    val accessToken: String,
    val expiresAt: Instant,
    val email: String,
) {
    override fun toString(): String = "AdminLoginResponse(accessToken=[REDACTED], expiresAt=$expiresAt, email=$email)"

    companion object {
        fun from(result: AdminLogin): AdminLoginResponse =
            AdminLoginResponse(accessToken = result.accessToken, expiresAt = result.expiresAt, email = result.email)
    }
}

/** 현재 어드민 정보 응답. */
data class AdminMeResponse(
    val email: String,
)

/** 등록되지 않은 어드민 로그인의 403(4032) 응답 data. 본인이 운영자에게 전달할 등록 정보다. */
data class AdminRegistrationResponse(
    val email: String,
    val googleSub: String,
)
