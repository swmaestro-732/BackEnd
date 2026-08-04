package com.example.backend.bootstrap.exception

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
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

    /** 요청 본문 파싱 실패(JSON 손상, 필수 필드 누락, enum 값 오류 등) → 400. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, "요청 본문 형식이 올바르지 않습니다.")

    /**
     * 잘못된 요청 인자 → 400.
     * 메시지가 그대로 클라이언트에 노출되므로, `require(...)` 로 던질 때는
     * 반드시 사용자에게 보여줄 한국어 문구를 담아야 한다(예: "유효하지 않은 커서입니다.").
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, e.message)

    /** 경로/쿼리 파라미터 타입 불일치(예: 숫자 자리에 문자) → 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, "요청 파라미터 형식이 올바르지 않습니다: ${e.name}")

    /** 필수 요청 파라미터 누락(required = true) → 400. */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Nothing?>> =
        respond(ErrorCode.INVALID_INPUT, "필수 요청 파라미터가 누락되었습니다: ${e.parameterName}")

    /**
     * 조회 실패 등 → 404.
     * `first()`/`single()` 등 표준 라이브러리가 던지는 영어 메시지가 노출되지 않도록
     * [ErrorCode.NOT_FOUND]의 고정 메시지를 사용한다(리소스별 문구가 필요하면 [BusinessException]으로).
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ApiResponse<Nothing?>> = respond(ErrorCode.NOT_FOUND)

    /** DB UNIQUE 위반 중 제약 대상을 식별할 수 있는 사용자 중복만 409로 변환한다. */
    @ExceptionHandler(ExposedSQLException::class)
    fun handleSqlException(e: ExposedSQLException): ResponseEntity<ApiResponse<Nothing?>> {
        if (e.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
            val message = e.message?.lowercase().orEmpty()
            val errorCode =
                when {
                    "handle" in message -> ErrorCode.HANDLE_ALREADY_TAKEN
                    "nickname" in message -> ErrorCode.NICKNAME_ALREADY_TAKEN
                    "saved_courses" in message -> ErrorCode.COURSE_ALREADY_SAVED
                    else -> null
                }
            if (errorCode != null) {
                return respond(errorCode)
            }
        }
        return handleUnexpected(e)
    }

    /** 그 외 → 500. 원인 추적을 위해 스택트레이스를 남긴다. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ApiResponse<Nothing?>> {
        // 스프링 시큐리티 인증/인가 예외(@PreAuthorize·@CurrentUserId 등)는 필터의
        // ExceptionTranslationFilter 가 401/403 으로 변환하도록 재던진다(500 로 삼키지 않는다).
        if (e is AuthenticationException || e is AccessDeniedException) throw e
        log.error("처리되지 않은 예외", e)
        return respond(ErrorCode.INTERNAL_ERROR)
    }

    private fun respond(
        errorCode: ErrorCode,
        message: String? = null,
        fieldErrors: List<ApiResponse.FieldError>? = null,
    ): ResponseEntity<ApiResponse<Nothing?>> =
        ResponseEntity.status(errorCode.status).body(ApiResponse.error(errorCode, message, fieldErrors))

    private companion object {
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }
}
