package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.bootstrap.config.AiChatSessionProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AiChatErrorCode
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.PlaceSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.RegionSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.ThemeSlot
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import tools.jackson.module.kotlin.jacksonObjectMapper

/** [AiChatSessionStore] — Redis 가 있으면 setex 30분, 없으면 허용 시에만 메모리 보관하고 아니면 503 인 것을 검증한다. */
class AiChatSessionStoreTest {
    private val objectMapper = jacksonObjectMapper()

    private val session =
        AiChatSession(
            id = "s1",
            userId = 7L,
            step = AiChatStep.PLACE_CONFIRM,
            region = RegionSlot("성수", listOf("1120011400")),
            theme = ThemeSlot(null, "실내", listOf("DATE", "CAFETOUR")),
            place = null,
            placeCandidates = mapOf("place:10" to PlaceSlot(10L, "어니언 성수")),
            shownCourseIds = listOf(1L, 2L),
            llmCalls = 3,
        )

    @Suppress("UNCHECKED_CAST")
    private fun providerOf(client: RedisClient?): ObjectProvider<RedisClient> {
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<RedisClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private val allowInMemory = AiChatSessionProperties(allowInMemory = true)

    @Test
    fun `Redis 가 없어도 메모리 폴백이 허용되면 프로세스 메모리에 두고 모든 슬롯을 그대로 되읽는다`() {
        val store = AiChatSessionStore(providerOf(null), objectMapper, allowInMemory)

        store.save(session)

        assertThat(store.find("s1")).isEqualTo(session)
        assertThat(store.find("missing")).isNull()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Redis 가 있으면 접두사 키로 30분 setex 하고 get 으로 되읽는다`() {
        val client = mock(RedisClient::class.java)
        val connection = mock(StatefulRedisConnection::class.java) as StatefulRedisConnection<String, String>
        val commands = mock(RedisCommands::class.java) as RedisCommands<String, String>
        `when`(client.connect()).thenReturn(connection)
        `when`(connection.sync()).thenReturn(commands)
        `when`(commands.get("ai-chat:session:s1")).thenReturn(objectMapper.writeValueAsString(session))
        val store = AiChatSessionStore(providerOf(client), objectMapper, AiChatSessionProperties())

        store.save(session)
        val found = store.find("s1")

        verify(commands).setex(eq("ai-chat:session:s1"), eq(1800L), anyString())
        assertThat(found).isEqualTo(session)
        assertThat(store.find("other")).isNull()
    }

    @Test
    fun `Redis 가 없고 메모리 폴백이 허용되지 않으면(운영 기본값) 저장·조회가 503 이다`() {
        val store = AiChatSessionStore(providerOf(null), objectMapper, AiChatSessionProperties())

        assertThatThrownBy { store.save(session) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
        assertThatThrownBy { store.find("s1") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `Redis 명령이 타임아웃 등으로 실패하면 500 이 아니라 503 이다`() {
        val client = mock(RedisClient::class.java)
        val connection = mock(StatefulRedisConnection::class.java) as StatefulRedisConnection<String, String>
        val commands = mock(RedisCommands::class.java) as RedisCommands<String, String>
        `when`(client.connect()).thenReturn(connection)
        `when`(connection.sync()).thenReturn(commands)
        `when`(commands.get(anyString())).thenThrow(RedisCommandTimeoutException("timeout"))
        `when`(commands.setex(anyString(), eq(1800L), anyString())).thenThrow(RedisCommandTimeoutException("timeout"))
        val store = AiChatSessionStore(providerOf(client), objectMapper, AiChatSessionProperties())

        assertThatThrownBy { store.find("s1") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
        assertThatThrownBy { store.save(session) }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(AiChatErrorCode.AI_CHAT_SESSION_UNAVAILABLE)
    }
}
