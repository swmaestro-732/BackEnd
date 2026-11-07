package com.example.backend.notification.adapter.outbound.user

import com.example.backend.user.application.port.inbound.UserUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class NotificationActorLookupAdapterTest {
    private val users = mock(UserUseCase::class.java)
    private val adapter = NotificationActorLookupAdapter(users)

    @Test
    fun `잠금은 user 유스케이스의 활성 사용자 공유 잠금에 위임한다`() {
        `when`(users.lockActiveShared(listOf(7L, 8L))).thenReturn(setOf(7L))

        assertThat(adapter.lockActive(listOf(7L, 8L))).containsExactly(7L)
    }
}
