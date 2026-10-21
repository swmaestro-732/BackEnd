package com.example.backend.chat.domain.model

/** 답글 대상 원문 스냅샷. 원문이 화면에 없어도 "○○님에게 답장: …" 을 보여 줄 수 있다. */
data class ChatReply(
    val seq: Long,
    val senderId: Long,
    val preview: String,
)
