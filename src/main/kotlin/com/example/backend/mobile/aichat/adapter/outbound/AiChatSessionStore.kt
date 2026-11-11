package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.mobile.aichat.application.port.outbound.AiChatSessionPort
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
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
 * Redis 가 없으면(로컬·CI, `redis.endpoint` 미설정) 프로세스 메모리에 같은 TTL 로 둔다 — 인스턴스가 여럿인 환경에서는 Redis 가 필수다.
 */
@Component
class AiChatSessionStore(
    private val redisClientProvider: ObjectProvider<RedisClient>,
    private val objectMapper: ObjectMapper,
) : AiChatSessionPort {
    private val redis: RedisCommands<String, String>? by lazy { redisClientProvider.ifAvailable?.connect()?.sync() }
    private val local = ConcurrentHashMap<String, LocalEntry>()

    override fun find(sessionId: String): AiChatSession? {
        val key = key(sessionId)
        val json = redis?.get(key) ?: localGet(key) ?: return null
        return objectMapper.readValue(json, AiChatSession::class.java)
    }

    override fun save(session: AiChatSession) {
        val key = key(session.id)
        val json = objectMapper.writeValueAsString(session)
        val commands = redis
        if (commands != null) {
            commands.setex(key, TTL.seconds, json)
        } else {
            local[key] = LocalEntry(Instant.now() + TTL, json)
            purgeExpired()
        }
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
