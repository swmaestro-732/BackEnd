package com.example.backend.admin.application.port.inbound.dto

import java.time.Instant

/** 어드민 로그인 결과. 구글 인증에 성공하면 등록 여부에 따라 둘 중 하나다. */
sealed interface AdminLogin {
    val email: String

    /** 이메일과 구글 계정이 함께 등록돼 어드민 토큰을 받았다. 토큰은 로그에 남지 않게 toString 에서 가린다. */
    data class LoggedIn(
        override val email: String,
        val accessToken: String,
        val expiresAt: Instant,
    ) : AdminLogin {
        override fun toString(): String = "LoggedIn(email=$email, accessToken=[REDACTED], expiresAt=$expiresAt)"
    }

    /** 아직 등록되지 않았다. 본인이 [email] 과 [googleSub] 를 운영자에게 전달해 등록을 요청한다. */
    data class RegistrationRequired(
        override val email: String,
        val googleSub: String,
    ) : AdminLogin
}
