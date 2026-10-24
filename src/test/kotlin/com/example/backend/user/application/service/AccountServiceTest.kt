package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.UserErrorCode
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.user.application.event.UserActivityEvent
import com.example.backend.user.application.port.inbound.UserActivityKind
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.LikeThemePort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

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

    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val occurredAt = Instant.parse("2026-10-24T10:09:59Z")
    private val clock = Clock.fixed(occurredAt, ZoneOffset.UTC)

    private val service =
        AccountService(
            userPersistencePort = userPersistencePort,
            userAreaPersistencePort = userAreaPersistencePort,
            followPersistencePort = followPersistencePort,
            userLikeThemePort = userLikeThemePort,
            mediaCleanupUseCase = mediaCleanupUseCase,
            userAreaResolver = UserAreaResolver(areaQueryUseCase),
            userLikeThemeResolver = UserLikeThemeResolver(likeThemePort),
            eventPublisher = eventPublisher,
            clock = clock,
        )

    @Test
    fun `updateProfile - 사용자를 찾을 수 없으면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.findById(1L)).thenReturn(null)

        val ex =
            assertThrows(BusinessException::class.java) {
                service.updateProfile(userId = 1L, command = UpdateProfileCommand())
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `새 팔로우를 생성하면 사용자 활동 이벤트를 한 번 발행한다`() {
        givenActiveUsers()
        `when`(followPersistencePort.follow(1L, 2L)).thenReturn(true)

        val result = service.follow(followerId = 1L, targetId = 2L)

        assertTrue(result.isFollowing)
        assertEquals(3, result.followersCnt)
        val order = inOrder(followPersistencePort, eventPublisher)
        order.verify(followPersistencePort).follow(1L, 2L)
        order.verify(eventPublisher).publishEvent(UserActivityEvent(UserActivityKind.FOLLOWED, 1L, 2L, occurredAt))
        verifyNoMoreInteractions(eventPublisher)
    }

    @Test
    fun `이미 팔로우한 대상이면 이벤트를 발행하지 않는다`() {
        givenActiveUsers()
        `when`(followPersistencePort.follow(1L, 2L)).thenReturn(false)

        val result = service.follow(followerId = 1L, targetId = 2L)

        assertTrue(result.isFollowing)
        assertEquals(3, result.followersCnt)
        verify(followPersistencePort).follow(1L, 2L)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `팔로우 저장에 실패하면 이벤트를 발행하지 않는다`() {
        givenActiveUsers()
        `when`(followPersistencePort.follow(1L, 2L)).thenThrow(IllegalStateException("저장 실패"))

        assertThrows(IllegalStateException::class.java) { service.follow(followerId = 1L, targetId = 2L) }

        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `follow - 팔로우 대상이 비활성이면 USER_NOT_FOUND 를 던진다`() {
        // followerId(1L)만 active → targetId(2L)가 active 에 없어 첫 번째 가드가 발동
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(1L))

        val ex = assertThrows(BusinessException::class.java) { service.follow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `follow - 팔로워가 비활성이면 USER_NOT_FOUND 를 던진다`() {
        // targetId(2L)는 active 하지만 followerId(1L)는 active 하지 않음
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(2L))

        val ex = assertThrows(BusinessException::class.java) { service.follow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `unfollow - 팔로우 대상이 비활성이면 USER_NOT_FOUND 를 던진다`() {
        // followerId(1L)만 active → targetId(2L)가 active 에 없어 첫 번째 가드가 발동
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(1L))

        val ex = assertThrows(BusinessException::class.java) { service.unfollow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `unfollow - 팔로워가 비활성이면 USER_NOT_FOUND 를 던진다`() {
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(2L))

        val ex = assertThrows(BusinessException::class.java) { service.unfollow(followerId = 1L, targetId = 2L) }

        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    private fun givenActiveUsers() {
        `when`(userPersistencePort.lockActive(listOf(1L, 2L))).thenReturn(setOf(1L, 2L))
        val profile = mock(UserProfileRow::class.java)
        `when`(profile.followersCnt).thenReturn(3)
        `when`(userPersistencePort.findProfile(2L)).thenReturn(profile)
    }
}
