package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.adapter.messaging.SavedPlaceIndexMessage
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.time.Instant

class SavedPlaceIndexSqsConsumerTest {
    private val port = mock(SavedPlaceSearchIndexPort::class.java)
    private val consumer = SavedPlaceIndexSqsConsumer(port)

    @Test
    fun `INDEX 메시지는 문서를 재구성해 색인 포트로 재처리한다`() {
        consumer.onMessage(
            SavedPlaceIndexMessage(
                op = SavedPlaceIndexMessage.OP_INDEX,
                userId = 7L,
                placeId = 42L,
                lat = 37.51,
                lon = 127.02,
                savedAtEpochMs = Instant.parse("2026-10-11T08:00:00Z").toEpochMilli(),
            ),
        )

        verify(port).index(
            SavedPlaceIndexDocument(7L, 42L, Coordinate(37.51, 127.02), Instant.parse("2026-10-11T08:00:00Z")),
        )
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
