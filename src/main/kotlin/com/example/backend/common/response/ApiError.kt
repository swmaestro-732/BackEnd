package com.example.backend.common.response

import java.time.OffsetDateTime

/** 표준 에러 응답 바디. (도메인 무관 공통 응답 타입) */
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val fieldErrors: List<FieldError> = emptyList(),
) {
    data class FieldError(
        val field: String,
        val reason: String,
    )
}
