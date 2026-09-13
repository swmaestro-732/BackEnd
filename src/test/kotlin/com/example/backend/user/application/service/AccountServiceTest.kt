package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.UserErrorCode
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.LikeThemePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * AccountService 순수 단위 테스트 — 커버되지 않은 에러 경로를 검증한다.
 */
class AccountServiceTest {
    private val userPersistencePort = mock(UserPersistencePort::class.java)
    private val userAreaPersistencePort = mock(UserAreaPersistencePort::class.java)
    private val followPersistencePort = mock(FollowPersistencePort::class.java)
    private val userLikeThemePort = mock(UserLikeThemePort::class.java)
    private val mediaCleanupUseCase = mock(MediaCleanupUseCase::class.java)
    private val areaQueryUseCase = mock(AreaQueryUseCase::class.java)
    private val likeThemePort =
        object : LikeThemePort {
            override fun listThemeNames(): List<String> = listOf("DATE", "CAFETOUR", "CULTURE")
        }

    private val service =
        AccountService(
            userPersistencePort = userPersistencePort,
            userAreaPersistencePort = userAreaPersistencePort,
            followPersistencePort = followPersistencePort,
            userLikeThemePort = userLikeThemePort,
            mediaCleanupUseCase = mediaCleanupUseCase,
            userAreaResolver = UserAreaResolver(areaQueryUseCase),
            userLikeThemeResolver = UserLikeThemeResolver(likeThemePort),
        )

    @Test
    fun `updateProfile - 사용자를 찾을 수 없으면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.findById(1L)).thenReturn(null)

        val ex =
            assertThrows(BusinessException::class.java) {
                service.updateProfile(userId = 1L, command = UpdateProfileCommand())
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `follow - 팔로우 대상이 비활성이면 USER_NOT_FOUND 를 던진다`() {
        // lockActive 가 빈 셋을 반환 → targetId(2L)가 active 에 없음
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(emptySet())

        val ex = assertThrows(BusinessException::class.java) { service.follow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `follow - 팔로워가 비활성이면 USER_NOT_FOUND 를 던진다`() {
        // targetId(2L)는 active 하지만 followerId(1L)는 active 하지 않음
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(2L))

        val ex = assertThrows(BusinessException::class.java) { service.follow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `unfollow - 팔로우 대상이 비활성이면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(emptySet())

        val ex = assertThrows(BusinessException::class.java) { service.unfollow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `unfollow - 팔로워가 비활성이면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(2L))

        val ex = assertThrows(BusinessException::class.java) { service.unfollow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
    }
}
