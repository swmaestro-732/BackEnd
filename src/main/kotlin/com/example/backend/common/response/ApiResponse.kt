package com.example.backend.common.response

/**
 * 모든 API 응답의 공통 엔벨로프. 성공·에러를 동일한 형태로 통일한다.
 * HTTP 상태코드(200/400/404/500)는 그대로 두고(RESTful), 바디만 이 형태로 감싼다.
 *
 * - 성공: `{ success:true, code:"OK", message:null, data:{...} }`
 * - 에러: `{ success:false, code:"...", message:"...", data:null, fieldErrors:[...] }`
 */
data class ApiResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String?,
    val data: T? = null,
    val fieldErrors: List<FieldError>? = null,
) {
    data class FieldError(
        val field: String,
        val reason: String,
    )

    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, code = "OK", message = null, data = data)

        /** 본문 없는 성공(204 등). */
        fun ok(): ApiResponse<Nothing?> = ApiResponse(success = true, code = "OK", message = null, data = null)

        fun error(
            errorCode: ErrorCode,
            message: String? = null,
            fieldErrors: List<FieldError>? = null,
        ): ApiResponse<Nothing?> =
            ApiResponse(
                success = false,
                code = errorCode.code,
                message = message ?: errorCode.message,
                data = null,
                fieldErrors = fieldErrors,
            )
    }
}
