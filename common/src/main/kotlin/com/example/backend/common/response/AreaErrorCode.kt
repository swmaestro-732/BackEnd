package com.example.backend.common.response

enum class AreaErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    AREA_NOT_FOUND(404, 4044, "지역을 찾을 수 없습니다."),
}
