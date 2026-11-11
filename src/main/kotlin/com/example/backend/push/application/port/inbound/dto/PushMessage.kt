package com.example.backend.push.application.port.inbound.dto

import java.time.Duration

/**
 * 대상 토큰을 포함한 푸시 발송 요청. 로그에는 토큰 값 대신 개수만 노출한다.
 * [androidTtl] 이 있으면 설정값 대신 그 기간만 오프라인 기기에 보관하고, 큐에서도 그보다 오래된 메시지는 버린다.
 * FCM 이 거부하는 빈 토큰, data 키, 크기 초과는 큐에 넣기 전에 IllegalArgumentException 으로 막는다.
 */
data class PushMessage(
    val tokens: List<String>,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val androidTtl: Duration? = null,
) {
    init {
        // FCM SDK 는 빈 토큰이 하나라도 있으면 묶음 전체를 거부하므로 큐에 넣기 전에 막는다.
        require(tokens.none { it.isBlank() }) { "빈 푸시 토큰은 쓸 수 없다" }
        for (key in data.keys) {
            val lowerKey = key.lowercase()
            require(lowerKey !in RESERVED_DATA_KEYS && RESERVED_DATA_KEY_PREFIXES.none { lowerKey.startsWith(it) }) {
                "FCM 예약 data 키는 쓸 수 없다: key=$key"
            }
        }
        // FCM 한도(4096 bytes)는 JSON 구조까지 포함하므로 내용만으로 여유 있게 근사한다.
        val payloadBytes = (sequenceOf(title, body) + data.keys + data.values).sumOf { it.toByteArray().size }
        require(payloadBytes <= MAX_PAYLOAD_BYTES) { "푸시 페이로드가 너무 크다: bytes=$payloadBytes" }
    }

    override fun toString(): String =
        "PushMessage(tokenCount=${tokens.size}, title=$title, body=$body, data=$data, androidTtl=$androidTtl)"

    private companion object {
        const val MAX_PAYLOAD_BYTES = 4000
        val RESERVED_DATA_KEYS = setOf("from", "notification", "message_type")
        val RESERVED_DATA_KEY_PREFIXES = listOf("google.", "gcm.")
    }
}
