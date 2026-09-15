package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.user.application.port.inbound.dto.FollowListCommand
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import com.example.backend.user.application.port.outbound.FollowUserRow
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class FollowQueryServiceTest {
    private val followPort = mock(FollowPersistencePort::class.java)
    private val userPort = mock(UserPersistencePort::class.java)
    private val service = FollowQueryService(followPort, userPort)

    private val profile =
        UserProfileRow(
            id = 10L,
            nickname = "현우",
            handle = "hyunwoo",
            profileImageUrl = null,
            bio = null,
            followersCnt = 5,
            followingsCnt = 3,
            publicCoursesCnt = 2,
            followerCoursesCnt = 0,
            privateCoursesCnt = 1,
        )

    private fun row(
        followId: Long,
        userId: Long,
        nickname: String,
    ) = FollowUserRow(followId = followId, userId = userId, nickname = nickname, handle = null, profileImageUrl = null)

    @Test
    fun `대상 사용자가 없으면 BusinessException 을 던진다`() {
        `when`(userPort.findProfile(99L)).thenReturn(null)
        val cmd = FollowListCommand(targetUserId = 99L, viewerId = null, cursor = null, size = 10)

        assertThrows<BusinessException> { service.getFollowers(cmd) }
    }

    @Test
    fun `getFollowers — 팔로워 목록과 totalCount 를 반환한다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        val rows = listOf(row(201, 1, "A"), row(200, 2, "B"))
        `when`(followPort.findFollowers(10L, null, 11)).thenReturn(rows)

        val cmd = FollowListCommand(targetUserId = 10L, viewerId = null, cursor = null, size = 10)
        val result = service.getFollowers(cmd)

        assertEquals(5L, result.totalCount) // followersCnt
        assertEquals(2, result.users.size)
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    fun `getFollowings — 팔로잉 목록과 totalCount 를 반환한다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        val rows = listOf(row(301, 3, "C"))
        `when`(followPort.findFollowings(10L, null, 11)).thenReturn(rows)

        val cmd = FollowListCommand(targetUserId = 10L, viewerId = null, cursor = null, size = 10)
        val result = service.getFollowings(cmd)

        assertEquals(3L, result.totalCount) // followingsCnt
        assertEquals(1, result.users.size)
    }

    @Test
    fun `rows 가 size+1 이면 hasNext=true 이고 nextCursor 가 마지막 항목의 followId 다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        // 11개 반환 → size(10) 보다 많음 → hasNext
        val rows = (11 downTo 1).map { row(it.toLong(), it.toLong(), "user$it") }
        `when`(followPort.findFollowers(10L, null, 11)).thenReturn(rows)

        val cmd = FollowListCommand(targetUserId = 10L, viewerId = null, cursor = null, size = 10)
        val result = service.getFollowers(cmd)

        assertTrue(result.hasNext)
        assertEquals(10, result.users.size)
        // nextCursor = last of page (index 9) = followId of row(2)
        assertEquals("2", result.nextCursor)
    }

    @Test
    fun `유효하지 않은 cursor 는 BusinessException 을 던진다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        val cmd = FollowListCommand(targetUserId = 10L, viewerId = null, cursor = "not-a-number", size = 10)

        assertThrows<BusinessException> { service.getFollowers(cmd) }
    }

    @Test
    fun `cursor 가 있으면 숫자로 디코드해 port 에 전달한다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        `when`(followPort.findFollowers(10L, 100L, 11)).thenReturn(emptyList())

        val cmd = FollowListCommand(targetUserId = 10L, viewerId = null, cursor = "100", size = 10)
        val result = service.getFollowers(cmd)

        verify(followPort).findFollowers(10L, 100L, 11) // 디코딩된 cursor(100L)와 size+1(11)이 그대로 전달됨
        assertFalse(result.hasNext)
        assertTrue(result.users.isEmpty())
    }

    @Test
    fun `viewerId 가 있으면 isFollowing isFollower 가 배선된다`() {
        `when`(userPort.findProfile(10L)).thenReturn(profile)
        val rows = listOf(row(201, 5L, "D"))
        `when`(followPort.findFollowers(10L, null, 11)).thenReturn(rows)
        `when`(followPort.filterFollowing(1L, listOf(5L))).thenReturn(setOf(5L))
        `when`(followPort.filterFollowers(1L, listOf(5L))).thenReturn(emptySet())

        val cmd = FollowListCommand(targetUserId = 10L, viewerId = 1L, cursor = null, size = 10)
        val result = service.getFollowers(cmd)

        val user = result.users.first()
        assertTrue(user.isFollowing)
        assertFalse(user.isFollower)
    }
}
