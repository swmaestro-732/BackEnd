package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.bootstrap.config.AiChatSessionProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AiChatErrorCode
import com.example.backend.mobile.aichat.application.port.outbound.AiChatSessionPort
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 아웃바운드 어댑터 — 대화 세션을 Redis 에 JSON 으로 30분 보관한다(저장마다 TTL 갱신).
 * Redis 가 없으면(`redis.endpoint` 미설정) 기본은 503(`AI_CHAT_SESSION_UNAVAILABLE`) — 인스턴스가 여럿인 운영에서
 * 프로세스 메모리 세션은 요청마다 어긋나므로 쓰지 않는다. 로컬·CI 는 `ai-chat.session.allow-in-memory` 로 메모리 폴백을 켠다.
 */
@Component
class AiChatSessionStore(
    private val redisClientProvider: ObjectProvider<RedisClient>,
    private val objectMapper: ObjectMapper,
    private val properties: AiChatSessionProperties,
) : AiChatSessionPort {
    private val log = KotlinLogging.logger {}
    private val redis: RedisCommands<String, String>? by lazy {
        redisClientProvider.ifAvailable?.connect()?.sync().also { commands ->
            if (commands == null && properties.allowInMemory) {
                log.warn { "ai-chat 세션: Redis 미설정 → 프로세스 메모리 보관(allow-in-memory). 다중 인스턴스에서는 쓰지 말 것." }
            }
        }
    }
    private val local = ConcurrentHashMap<String, LocalEntry>()

    override fun find(sessionId: String): AiChatSession? {
        val key = key(sessionId)
        val json = redisOrNull()?.let { commands -> redisCall { commands.get(key) } } ?: localGet(key) ?: return null
        return objectMapper.readValue(json, AiChatSession::class.java)
    }

    override fun save(session: AiChatSession) {
        val key = key(session.id)
        val json = objectMapper.writeValueAsString(session)
        val commands = redisOrNull()
        if (commands != null) {
            redisCall { commands.setex(key, TTL.seconds, json) }
        } else {
            local[key] = LocalEntry(Instant.now() + TTL, json)
            purgeExpired()
        }
    }

    /** Redis 명령 — 없으면 메모리 폴백이 허용된 경우에만 null, 아니면 503. 연결 자체가 실패해도 503. */
    private fun redisOrNull(): RedisCommands<String, String>? =
        redisCall { redis }
            ?: if (properties.allowInMemory) {
                null
            } else {
                throw BusinessException(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
            }

    /** Redis 연결·명령 실패(타임아웃, 끊김)는 500 대신 503 — 앱은 잠시 후 다시 시도 안내를 띄운다. */
    private fun <T> redisCall(block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            log.warn { "ai-chat 세션 Redis 실패(503): ${e.javaClass.simpleName}" }
            throw BusinessException(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
        }

    private fun localGet(key: String): String? {
        val entry = local[key] ?: return null
        if (entry.expiresAt.isBefore(Instant.now())) {
            local.remove(key)
            return null
        }
        return entry.json
    }

    private fun purgeExpired() {
        val now = Instant.now()
        local.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    private fun key(sessionId: String) = "$KEY_PREFIX$sessionId"

    private data class LocalEntry(
        val expiresAt: Instant,
        val json: String,
    )

    private companion object {
        const val KEY_PREFIX = "ai-chat:session:"
        val TTL: Duration = Duration.ofMinutes(30)
    }
}
