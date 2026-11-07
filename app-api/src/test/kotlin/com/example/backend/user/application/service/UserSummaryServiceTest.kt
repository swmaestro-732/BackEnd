package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserSummaryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class UserSummaryServiceTest {
    private val port = mock(UserSummaryPort::class.java)
    private val service = UserSummaryService(port)

    @Test
    fun `빈 ids 를 넘기면 port 를 호출하지 않고 빈 목록을 반환한다`() {
        val result = service.findSummaries(emptyList())

        assertTrue(result.isEmpty())
        verify(port, never()).findByIds(emptyList())
    }

    @Test
    fun `ids 가 있으면 port 에 위임하고 결과를 그대로 반환한다`() {
        val summaries =
            listOf(
                UserSummaryUseCase.UserSummary(id = 1L, nickname = "현우", profileImageUrl = null),
                UserSummaryUseCase.UserSummary(id = 2L, nickname = "지은", profileImageUrl = "https://example.com/2.jpg"),
            )
        `when`(port.findByIds(listOf(1L, 2L))).thenReturn(summaries)

        val result = service.findSummaries(listOf(1L, 2L))

        assertEquals(2, result.size)
        assertEquals("현우", result[0].nickname)
        assertEquals("지은", result[1].nickname)
    }
}
