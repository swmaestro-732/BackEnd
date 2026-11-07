package com.example.backend.course.adapter.inbound.event

import com.example.backend.course.application.port.inbound.UserWithdrawalCleanupUseCase
import com.example.backend.user.application.event.UserWithdrawnEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class UserWithdrawnEventListenerTest {
    private val userWithdrawalCleanupUseCase = mock(UserWithdrawalCleanupUseCase::class.java)
    private val listener = UserWithdrawnEventListener(userWithdrawalCleanupUseCase)

    @Test
    fun `탈퇴 이벤트를 course 정리 유스케이스로 넘긴다`() {
        listener.onUserWithdrawn(UserWithdrawnEvent(userId = 7L, savedCourseIds = listOf(10L)))

        verify(userWithdrawalCleanupUseCase).cleanUpWithdrawnUser(7L, listOf(10L))
    }
}
