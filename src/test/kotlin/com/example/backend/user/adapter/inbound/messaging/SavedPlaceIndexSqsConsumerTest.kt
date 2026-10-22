package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.adapter.messaging.SavedPlaceIndexMessage
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

class SavedPlaceIndexSqsConsumerTest {
    private val port = mock(SavedPlaceSearchIndexPort::class.java)
    private val consumer = SavedPlaceIndexSqsConsumer(port)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `INDEX 메시지는 방문 여부를 보존해 색인 포트로 재처리한다`(visited: Boolean) {
        consumer.onMessage(
            SavedPlaceIndexMessage(
                op = SavedPlaceIndexMessage.OP_INDEX,
                userId = 7L,
                placeId = 42L,
                lat = 37.51,
                lon = 127.02,
                savedAtEpochMs = Instant.parse("2026-10-11T08:00:00Z").toEpochMilli(),
                visited = visited,
                name = "어니언 성수",
                category = "CAFE",
                imageUrl = "https://img/42.jpg",
                rating = 4.8,
                ratingCount = 212,
            ),
        )

        verify(port).index(
            SavedPlaceIndexDocument(
                userId = 7L,
                placeId = 42L,
                location = Coordinate(37.51, 127.02),
                savedAt = Instant.parse("2026-10-11T08:00:00Z"),
                visited = visited,
                name = "어니언 성수",
                category = "CAFE",
                imageUrl = "https://img/42.jpg",
                rating = 4.8,
                ratingCount = 212,
            ),
        )
    }

    @Test
    fun `visited 가 없는 기존 메시지는 미방문으로 역직렬화한다`() {
        val message =
            jacksonObjectMapper().readValue(
                """{"op":"INDEX","userId":7,"placeId":42}""",
                SavedPlaceIndexMessage::class.java,
            )

        assertFalse(message.visited)
    }

    @Test
    fun `DELETE 메시지는 복합 키로 삭제를 재처리한다`() {
        consumer.onMessage(SavedPlaceIndexMessage(op = SavedPlaceIndexMessage.OP_DELETE, userId = 7L, placeId = 42L))

        verify(port).delete(7L, 42L)
    }

    @Test
    fun `알 수 없는 연산은 포트를 건드리지 않고 무시한다`() {
        assertDoesNotThrow {
            consumer.onMessage(SavedPlaceIndexMessage(op = "UNKNOWN", userId = 7L, placeId = 42L))
        }
        verifyNoInteractions(port)
    }
}
