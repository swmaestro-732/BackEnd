package com.example.backend.bootstrap.exception

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * 모든 예외를 [ApiResponse] 에러 엔벨로프로 통일한다.
 * HTTP 상태코드는 [ErrorCode.status] 로 매핑하고, 바디는 항상 `{ code, message, ... }`.
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /** 도메인/애플리케이션이 던진 비즈니스 예외 → ErrorCode 기준 응답. */
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ResponseEntity<ApiResponse<Nothing?>> = respond(e.errorCode, e.message)

    /** Bean Validation 실패(@RequestBody) → 400 + 필드별 사유. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing?>> {
        val fieldErrors =
            e.bindingResult.fieldErrors.map {
                ApiResponse.FieldError(it.field, it.defaultMessage ?: "invalid")
            }
        return respond(ErrorCode.VALIDATION_FAILED, fieldErrors = fieldErrors)
    }

    /** Bean Validation 실패(@RequestParam/@PathVariable 등 핸들러 파라미터) → 400 + 필드별 사유. */
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleParamValidation(e: HandlerMethodValidationException): ResponseEntity<ApiResponse<Nothing?>> {
        val fieldErrors =
            e.parameterValidationResults.flatMap { result ->
                result.resolvableErrors.map {
                    ApiResponse.FieldError(
                        result.methodParameter.parameterName ?: "unknown",
                        it.defaultMessage ?: "invalid",
                    )
                }
            }
        return respond(ErrorCode.VALIDATION_FAILED, fieldErrors = fieldErrors)
    }

    /** 잘못된 요청 인자 → 400. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, e.message)

    /** 경로/쿼리 파라미터 타입 불일치(예: placeId에 문자) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, "요청 파라미터 형식이 올바르지 않습니다: ${e.name}")

    /** 조회 실패 등 → 404. */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.NOT_FOUND, e.message)

    /** 그 외 → 500. 원인 추적을 위해 스택트레이스를 남긴다. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        log.error("처리되지 않은 예외", e)
        return respond(ErrorCode.INTERNAL_ERROR)
    }

    private fun respond(
        errorCode: ErrorCode,
        message: String? = null,
        fieldErrors: List<ApiResponse.FieldError>? = null,
    ): ResponseEntity<ApiResponse<Nothing?>> =
        ResponseEntity.status(errorCode.status).body(ApiResponse.error(errorCode, message, fieldErrors))
}
