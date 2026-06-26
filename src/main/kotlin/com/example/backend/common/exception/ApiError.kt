package com.example.backend.common.exception

import java.time.OffsetDateTime

/** 표준 에러 응답 바디. */
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
