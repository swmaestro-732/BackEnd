package com.example.backend.admin.application.port.outbound

/** 어드민 신원 확인 포트. 구글 ID 토큰을 검증하고 인증된 계정을 돌려준다. */
interface AdminIdentityPort {
    fun verifyGoogle(idToken: String): VerifiedGoogleAccount
}

/** 구글이 확인한 계정. [email] 은 소문자, [sub] 는 바뀌거나 재사용되지 않는 구글 계정 id. */
data class VerifiedGoogleAccount(
    val email: String,
    val sub: String,
)
