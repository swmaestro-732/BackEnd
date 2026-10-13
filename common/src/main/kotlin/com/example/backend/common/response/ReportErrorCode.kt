package com.example.backend.common.response

enum class ReportErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    // 400 은 4005(INVALID_APP_HEADER)까지 선점 — 다음 번호 4006 으로 채번했다.
    CANNOT_REPORT_SELF(400, 4006, "본인의 콘텐츠는 신고할 수 없습니다."),

    // 409 는 4098(COURSE_ALREADY_LIKED)까지 선점 — 다음 번호 4099 로 채번했다.
    REPORT_ALREADY_EXISTS(409, 4099, "이미 신고한 대상입니다."),
}
