package com.example.backend.notification.adapter.outbound.user

import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NotificationActorLookupAdapterTest {
    private val users = mock(UserUseCase::class.java)
    private val summaries = mock(UserSummaryUseCase::class.java)
    private val adapter = NotificationActorLookupAdapter(users, summaries)

    @Test
    fun `행위자 요약에서 닉네임을 반환한다`() {
        `when`(summaries.findSummaries(listOf(7L)))
            .thenReturn(listOf(UserSummaryUseCase.UserSummary(7L, "산책자", "https://example.com/profile.jpg")))

        assertThat(adapter.findNickname(7L)).isEqualTo("산책자")
        verify(summaries).findSummaries(listOf(7L))
    }

    @Test
    fun `행위자가 조회 결과에 없으면 null을 반환한다`() {
        `when`(summaries.findSummaries(listOf(7L))).thenReturn(emptyList())

        assertThat(adapter.findNickname(7L)).isNull()
    }

    @Test
    fun `잠금은 user 유스케이스의 활성 사용자 공유 잠금에 위임한다`() {
        `when`(users.lockActiveShared(listOf(7L, 8L))).thenReturn(setOf(7L))

        assertThat(adapter.lockActive(listOf(7L, 8L))).containsExactly(7L)
    }
}
