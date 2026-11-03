package com.example.backend.push.application.port.inbound.dto

/** 대상 토큰을 포함한 푸시 발송 요청. 로그에는 토큰 값 대신 개수만 노출한다. */
data class PushMessage(
    val tokens: List<String>,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
) {
    override fun toString(): String = "PushMessage(tokenCount=${tokens.size}, title=$title, body=$body, data=$data)"
}
