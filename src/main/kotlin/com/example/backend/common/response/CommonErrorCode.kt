package com.example.backend.common.response

enum class CommonErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    INVALID_INPUT(400, 4001, "잘못된 요청입니다."),
    VALIDATION_FAILED(400, 4002, "입력값 검증에 실패했습니다."),
    SOCIAL_AUTHENTICATION_FAILED(401, 4010, "소셜 인증에 실패했습니다."),
    INVALID_REGISTRATION_TOKEN(401, 4011, "유효하지 않은 회원가입 토큰입니다."),
    INVALID_REFRESH_TOKEN(401, 4012, "유효하지 않은 리프레시 토큰입니다."),
    ACCOUNT_SUSPENDED(403, 4030, "정지된 계정입니다."),
    ACCOUNT_INACTIVE(403, 4031, "이용할 수 없는 계정입니다."),
    NOT_FOUND(404, 4040, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, 4050, "허용되지 않은 HTTP 메서드입니다."),
    SOCIAL_ACCOUNT_ALREADY_REGISTERED(409, 4090, "이미 가입된 소셜 계정입니다."),
    UNSUPPORTED_MEDIA_TYPE(415, 4150, "지원하지 않는 파일 형식입니다."),
    PAYLOAD_TOO_LARGE(413, 4130, "업로드 가능한 파일 크기를 초과했습니다."),
    INTERNAL_ERROR(500, 5000, "서버 오류가 발생했습니다."),
}
