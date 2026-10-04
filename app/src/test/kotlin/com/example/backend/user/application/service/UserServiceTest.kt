package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.UserErrorCode
import com.example.backend.user.application.port.outbound.CourseCleanupPort
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserServiceTest {
    private val userPersistencePort = mock(UserPersistencePort::class.java)
    private val followPersistencePort = mock(FollowPersistencePort::class.java)
    private val refreshTokenPort = mock(RefreshTokenPort::class.java)
    private val savedCoursePersistencePort = mock(SavedCoursePersistencePort::class.java)
    private val userAreaPersistencePort = mock(UserAreaPersistencePort::class.java)
    private val userLikeThemePort = mock(UserLikeThemePort::class.java)
    private val courseCleanupPort = mock(CourseCleanupPort::class.java)

    private val service =
        UserService(
            userPersistencePort = userPersistencePort,
            followPersistencePort = followPersistencePort,
            refreshTokenPort = refreshTokenPort,
            savedCoursePersistencePort = savedCoursePersistencePort,
            userAreaPersistencePort = userAreaPersistencePort,
            userLikeThemePort = userLikeThemePort,
            courseCleanupPort = courseCleanupPort,
        )

    @Test
    fun `getProfile - 사용자를 찾을 수 없으면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.findProfile(99L)).thenReturn(null)

        val ex = assertThrows(BusinessException::class.java) { service.getProfile(userId = 99L, viewerId = null) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `getProfileByHandle - handle 을 찾을 수 없으면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.findByHandle("unknown")).thenReturn(null)

        val ex =
            assertThrows(BusinessException::class.java) {
                service.getProfileByHandle(handle = "unknown", viewerId = null)
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `withdraw - 비활성 사용자는 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.lockActive(listOf(1L))).thenReturn(emptySet())

        val ex = assertThrows(BusinessException::class.java) { service.withdraw(userId = 1L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }
}
