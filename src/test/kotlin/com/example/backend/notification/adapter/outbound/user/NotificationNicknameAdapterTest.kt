package com.example.backend.notification.adapter.outbound.user

import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.inbound.UserSummaryUseCase.UserSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class NotificationNicknameAdapterTest {
    private val summaries = mock(UserSummaryUseCase::class.java)
    private val adapter = NotificationNicknameAdapter(summaries)

    @Test
    fun `조회된 사용자만 id 별 닉네임으로 돌려준다`() {
        `when`(summaries.findSummaries(setOf(7L, 8L)))
            .thenReturn(listOf(UserSummary(7L, "산책자", "https://example.com/profile.jpg")))

        assertThat(adapter.findNicknames(setOf(7L, 8L))).containsExactlyEntriesOf(mapOf(7L to "산책자"))
    }
}
