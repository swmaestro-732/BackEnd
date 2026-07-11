package com.example.backend.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 모든 API 응답의 공통 엔벨로프. 성공·에러를 동일한 형태로 통일한다.
 * HTTP 상태코드(200/400/404/500)는 그대로 두고(RESTful), 바디만 이 형태로 감싼다.
 *
 * 클라이언트는 [code] 로 성공/실패를 판별한다(성공 [SUCCESS_CODE], 에러는 [ErrorCode]).
 * `data`/`fieldErrors` 는 값이 있을 때만 직렬화되고, `code`/`message` 는 항상 내려간다.
 *
 * - 성공: `{ code:2000, message:null, data:{...} }`
 * - 에러: `{ code:4002, message:"...", fieldErrors:[...] }`
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val fieldErrors: List<FieldError>? = null,
) {
    data class FieldError(
        val field: String,
        val reason: String,
    )

    companion object {
        /** 성공 코드. 에러 코드는 [ErrorCode] 가 소유한다. */
        const val SUCCESS_CODE = 2000

        fun <T> success(
            data: T,
            message: String? = null,
        ): ApiResponse<T> = ApiResponse(code = SUCCESS_CODE, message = message, data = data)

        /** 본문 없는 성공(204 등). */
        fun ok(): ApiResponse<Nothing?> = ApiResponse(code = SUCCESS_CODE, message = null)

        fun error(
            errorCode: ErrorCode,
            message: String? = null,
            fieldErrors: List<FieldError>? = null,
        ): ApiResponse<Nothing?> =
            ApiResponse(
                code = errorCode.code,
                message = message ?: errorCode.message,
                fieldErrors = fieldErrors,
            )
    }
}
