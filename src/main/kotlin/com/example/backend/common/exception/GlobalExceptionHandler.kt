package com.example.backend.common.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    /** Bean Validation 실패 → 400 + 필드별 사유. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val fieldErrors =
            e.bindingResult.fieldErrors.map {
                ApiError.FieldError(it.field, it.defaultMessage ?: "invalid")
            }
        return build(HttpStatus.BAD_REQUEST, "유효성 검사 실패", request, fieldErrors)
    }

    /** 조회 실패 등 → 404. */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        e: NoSuchElementException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = build(HttpStatus.NOT_FOUND, e.message ?: "찾을 수 없음", request)

    /** 잘못된 요청 인자 → 400. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        e: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = build(HttpStatus.BAD_REQUEST, e.message ?: "잘못된 요청", request)

    /** 그 외 → 500. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> = build(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다", request)

    private fun build(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        fieldErrors: List<ApiError.FieldError> = emptyList(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI,
                fieldErrors = fieldErrors,
            ),
        )
}
