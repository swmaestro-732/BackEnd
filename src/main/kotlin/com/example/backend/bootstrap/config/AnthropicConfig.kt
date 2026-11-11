package com.example.backend.bootstrap.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Claude API 클라이언트. `ai-chat.llm.api-key` 가 있고 `ai-chat.llm.enabled` 일 때만 만든다 —
 * 없으면 [AnthropicClient] 를 주입받는 어댑터가 `ObjectProvider` 로 부재를 감지해 규칙 기반으로 넘어간다.
 * 짧은 분류 호출이라 재시도 없이 타임아웃만 건다(사용자가 기다리는 요청 경로).
 */
@Configuration
@ConditionalOnExpression(
    "'\${ai-chat.llm.api-key:}'.trim().length() > 0 && \${ai-chat.llm.enabled:true}",
)
class AnthropicConfig(
    private val properties: AiChatLlmProperties,
) {
    @Bean(destroyMethod = "close")
    fun anthropicClient(): AnthropicClient =
        AnthropicOkHttpClient
            .builder()
            .apiKey(properties.apiKey.trim())
            .timeout(Duration.ofMillis(properties.timeoutMillis))
            .maxRetries(0)
            .build()
}
