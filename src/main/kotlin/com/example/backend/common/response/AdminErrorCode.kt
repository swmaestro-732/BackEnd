package com.example.backend.common.response

enum class AdminErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    // 4030(ACCOUNT_SUSPENDED), 4031(ACCOUNT_INACTIVE)은 CommonErrorCode 가 선점 — 다음 번호 4032 로 채번했다.
    ADMIN_FORBIDDEN(403, 4032, "관리자 계정이 아닙니다."),
}
