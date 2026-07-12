package com.example.backend.common.response

/**
 * 에러 코드 중앙관리 enum — 다양한 에러를 한 곳에서 정의한다.
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
    val code: String,
    val message: String,
) {
    // ── 공통 ──
    INVALID_INPUT(400, "COMMON_400_1", "잘못된 요청입니다."),
    VALIDATION_FAILED(400, "COMMON_400_2", "입력값 검증에 실패했습니다."),
    NOT_FOUND(404, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "COMMON_405", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_ERROR(500, "COMMON_500", "서버 오류가 발생했습니다."),

    // ── 코스(course) ──
    COURSE_NOT_FOUND(404, "COURSE_404", "코스를 찾을 수 없습니다."),

    // ── 사용자(user) ──
    USER_NOT_FOUND(404, "USER_404", "사용자를 찾을 수 없습니다."),
}
