package com.example.backend.common.exception

import com.example.backend.common.response.ErrorCode

/**
 * 비즈니스 예외 — [ErrorCode] 를 담아 던진다.
 * Spring 에 의존하지 않으므로 도메인/애플리케이션 계층 어디서나 사용 가능하다.
 * bootstrap 의 GlobalExceptionHandler 가 잡아 ErrorCode 기준으로 응답을 만든다.
 */
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
