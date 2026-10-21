package com.example.backend.common.response

enum class ChatErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    // 400 은 4006(CANNOT_REPORT_SELF)까지 선점 — 다음 번호부터 채번했다.
    CHAT_REGION_UNSUPPORTED(400, 4007, "채팅을 지원하지 않는 지역입니다."),
    CHAT_NEARBY_ROOM_LIMIT_EXCEEDED(400, 4008, "인접 지역 채팅방은 최대 3개까지 참여할 수 있습니다."),

    // 403 은 4031(ACCOUNT_INACTIVE)까지 선점.
    CHAT_ROOM_OUT_OF_RANGE(403, 4032, "현재 위치에서 참여할 수 없는 채팅방입니다."),
}
