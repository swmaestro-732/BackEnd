package com.example.backend.push.adapter.messaging

import com.example.backend.push.application.port.inbound.dto.PushMessage
import java.time.Instant

/**
 * 발행과 수신 어댑터가 공유하는 SQS 직렬화 계약. 토큰은 로그에 노출하지 않는다.
 * [createdAt] 이 없는 이전 형식 메시지는 수신 측에서 오래되지 않은 것으로 본다.
 */
data class PushQueueMessage(
    val tokens: List<String>,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Instant? = null,
) {
    constructor(message: PushMessage, createdAt: Instant) :
        this(message.tokens, message.title, message.body, message.data, createdAt)

    fun toPushMessage(): PushMessage = PushMessage(tokens, title, body, data)

    override fun toString(): String =
        "PushQueueMessage(tokenCount=${tokens.size}, title=$title, body=$body, data=$data, createdAt=$createdAt)"
}
