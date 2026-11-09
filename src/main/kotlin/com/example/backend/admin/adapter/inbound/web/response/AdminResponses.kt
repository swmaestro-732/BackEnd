package com.example.backend.admin.adapter.inbound.web.response

import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * 어드민 로그인 응답. [result] 가 LOGGED_IN 이면 토큰과 만료 시각을, REGISTRATION_REQUIRED 면 등록 요청에 쓸 googleSub 를 채운다.
 * 해당 없는 필드는 공통 엔벨로프처럼 응답에서 생략한다.
 * 이메일이 없는 경우와 이메일은 있지만 다른 구글 계정인 경우는 같은 REGISTRATION_REQUIRED 라 어드민 이메일을 알아낼 수 없다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdminLoginResponse(
    val result: Result,
    val email: String,
    val accessToken: String?,
    val expiresAt: Instant?,
    val googleSub: String?,
) {
    enum class Result { LOGGED_IN, REGISTRATION_REQUIRED }

    override fun toString(): String =
        "AdminLoginResponse(result=$result, email=$email, accessToken=${accessToken?.let { "[REDACTED]" }}, " +
            "expiresAt=$expiresAt, googleSub=$googleSub)"

    companion object {
        fun from(login: AdminLogin): AdminLoginResponse =
            when (login) {
                is AdminLogin.LoggedIn -> {
                    AdminLoginResponse(Result.LOGGED_IN, login.email, login.accessToken, login.expiresAt, null)
                }

                is AdminLogin.RegistrationRequired -> {
                    AdminLoginResponse(Result.REGISTRATION_REQUIRED, login.email, null, null, login.googleSub)
                }
            }
    }
}

/** 현재 어드민 정보 응답. */
data class AdminMeResponse(
    val email: String,
)
