package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * AI 채팅 슬롯 추출 LLM 설정. [apiKey] 가 비면 클라이언트 빈을 만들지 않아 규칙 기반 해석만 동작한다(fail-soft).
 * [maxCallsPerSession] 은 대화 한 세션당 호출 상한 — 자유 입력을 반복해 비용을 키우는 것을 막는다.
 * 호출은 OpenRouter(Messages API 호환)로 보낸다 — [baseUrl] 과 [bearerAuth] 기본값이 그 설정이다.
 */
@ConfigurationProperties(prefix = "ai-chat.llm")
data class AiChatLlmProperties(
    val enabled: Boolean = true,
    val apiKey: String = "",
    val model: String = "claude-haiku-5-5",
    val timeoutMillis: Long = 3000,
    val maxCallsPerSession: Int = 20,
    val baseUrl: String = "",
    val bearerAuth: Boolean = false,
)
