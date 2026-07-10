package com.example.backend.bootstrap.exception

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 모든 예외를 [ApiResponse] 에러 엔벨로프로 통일한다.
 * HTTP 상태코드는 [ErrorCode.status] 로 매핑하고, 바디는 항상 `{ success:false, code, message, ... }`.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    /** 도메인/애플리케이션이 던진 비즈니스 예외 → ErrorCode 기준 응답. */
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing?>> = respond(e.errorCode, e.message)

    /** Bean Validation 실패 → 400 + 필드별 사유. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing?>> {
        val fieldErrors =
            e.bindingResult.fieldErrors.map {
                ApiResponse.FieldError(it.field, it.defaultMessage ?: "invalid")
            }
        return respond(ErrorCode.VALIDATION_FAILED, fieldErrors = fieldErrors)
    }

    /** 잘못된 요청 인자 → 400. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, e.message)

    /** 조회 실패 등 → 404. */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.NOT_FOUND, e.message)

    /** 그 외 → 500. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing?>> = respond(ErrorCode.INTERNAL_ERROR)

    private fun respond(
        errorCode: ErrorCode,
        message: String? = null,
        fieldErrors: List<ApiResponse.FieldError>? = null,
    ): ResponseEntity<ApiResponse<Nothing?>> =
        ResponseEntity.status(errorCode.status).body(ApiResponse.error(errorCode, message, fieldErrors))
}
