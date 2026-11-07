package com.example.backend.common.response

/**
 * 에러 코드 공통 계약.
 *
 * `code` 는 내부 관리용 **숫자 코드**(HTTP 상태 + 일련번호, 예: 400 → 4001·4002)로,
 * 로깅·모니터링에서 번호 기준으로 집계하기 위함이다. 성공 코드(2000)는 ApiResponse 가 소유한다.
 *
 * `status` 는 Spring `HttpStatus` 가 아니라 **HTTP 숫자(Int)** 다.
 * → 도메인/애플리케이션 계층에서도 `BusinessException(ErrorCode)` 로 참조 가능
 *   (ArchUnit: 도메인은 spring 의존 금지 — 이 규칙을 깨지 않기 위함).
 * HTTP 매핑은 bootstrap 의 GlobalExceptionHandler 가 담당한다.
 */
interface ErrorCode {
    val status: Int
    val code: Int
    val message: String
}
