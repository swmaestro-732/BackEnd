package com.example.backend.common.response

enum class DirectionErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    DIRECTION_UNAVAILABLE(503, 5030, "도보 경로 서비스를 일시적으로 이용할 수 없습니다."),
}
