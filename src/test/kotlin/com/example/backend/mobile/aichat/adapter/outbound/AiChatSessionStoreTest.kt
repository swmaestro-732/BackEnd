package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.PlaceSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.RegionSlot
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession.ThemeSlot
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import tools.jackson.module.kotlin.jacksonObjectMapper

/** [AiChatSessionStore] — Redis 가 없으면 메모리에, 있으면 setex 30분으로 JSON 저장하고 되읽는 것을 검증한다. */
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

    @Test
    fun `Redis 가 없으면 프로세스 메모리에 두고 모든 슬롯을 그대로 되읽는다`() {
        val store = AiChatSessionStore(providerOf(null), objectMapper)

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
        val store = AiChatSessionStore(providerOf(client), objectMapper)

        store.save(session)
        val found = store.find("s1")

        verify(commands).setex(eq("ai-chat:session:s1"), eq(1800L), anyString())
        assertThat(found).isEqualTo(session)
        assertThat(store.find("other")).isNull()
    }
}
