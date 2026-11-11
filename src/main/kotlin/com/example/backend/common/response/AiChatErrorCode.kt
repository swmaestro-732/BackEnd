package com.example.backend.common.response

/** 코스 추천 AI 채팅(ai-chat) 에러 대역 4400~4499. */
enum class AiChatErrorCode(
    override val status: Int,
    override val code: Int,
    override val message: String,
) : ErrorCode {
    AI_CHAT_SESSION_NOT_FOUND(404, 4400, "대화가 만료되었습니다. 다시 시작해 주세요."),
    AI_CHAT_SESSION_UNAVAILABLE(503, 4401, "대화를 잠시 이용할 수 없습니다. 잠시 후 다시 시도해 주세요."),
}
