package com.example.backend.common.response

/**
 * 에러 코드 중앙관리 enum — 다양한 에러를 한 곳에서 정의한다.
 *
 * `code` 는 내부 관리용 **숫자 코드**(HTTP 상태 + 일련번호, 예: 400 → 4001·4002)로,
 * 로깅·모니터링에서 번호 기준으로 집계하기 위함이다. 성공 코드(2000)는 ApiResponse 가 소유한다.
 *
 * `status` 는 Spring `HttpStatus` 가 아니라 **HTTP 숫자(Int)** 다.
 * → 도메인/애플리케이션 계층에서도 `BusinessException(ErrorCode.X)` 로 참조 가능
 *   (ArchUnit: 도메인은 spring 의존 금지 — 이 규칙을 깨지 않기 위함).
 * HTTP 매핑은 bootstrap 의 GlobalExceptionHandler 가 담당한다.
 *
 * 도메인별 에러가 늘면 `ErrorCode` 를 인터페이스로 바꾸고 도메인별 enum(예: UserErrorCode)으로 확장한다.
 */
enum class ErrorCode(
    val status: Int,
    val code: Int,
    val message: String,
) {
    // ── 공통 ──
    INVALID_INPUT(400, 4001, "잘못된 요청입니다."),
    VALIDATION_FAILED(400, 4002, "입력값 검증에 실패했습니다."),
    SOCIAL_AUTHENTICATION_FAILED(401, 4010, "소셜 인증에 실패했습니다."),
    INVALID_REGISTRATION_TOKEN(401, 4011, "유효하지 않은 회원가입 토큰입니다."),
    INVALID_REFRESH_TOKEN(401, 4012, "유효하지 않은 리프레시 토큰입니다."),
    NOT_FOUND(404, 4040, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, 4050, "허용되지 않은 HTTP 메서드입니다."),
    SOCIAL_ACCOUNT_ALREADY_REGISTERED(409, 4090, "이미 가입된 소셜 계정입니다."),
    INTERNAL_ERROR(500, 5000, "서버 오류가 발생했습니다."),

    // ── 코스(course) ──
    COURSE_NOT_FOUND(404, 4041, "코스를 찾을 수 없습니다."),

    // ── 사용자(user) ──
    USER_NOT_FOUND(404, 4042, "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_TAKEN(409, 4091, "이미 사용 중인 닉네임입니다."),
    HANDLE_ALREADY_TAKEN(409, 4092, "이미 사용 중인 핸들입니다."),

    // ── 장소(place) ──
    PLACE_NOT_FOUND(404, 4043, "장소를 찾을 수 없습니다."),
}
