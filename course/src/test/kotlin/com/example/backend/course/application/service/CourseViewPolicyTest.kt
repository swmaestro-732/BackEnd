package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * 코스 열람 권한 정책 단위 테스트 — PUBLIC 전체, FOLLOWER 소유자·팔로워, PRIVATE 소유자만.
 */
class CourseViewPolicyTest {
    private val viewerInteractionPort = mock(ViewerInteractionPort::class.java)
    private val policy = CourseViewPolicy(viewerInteractionPort)

    private val owner = 1L
    private val other = 2L

    @Test
    fun `PUBLIC 은 누구나(비로그인 포함) 볼 수 있다`() {
        assertTrue(policy.isViewable(CourseVisibility.PUBLIC, owner, other))
        assertTrue(policy.isViewable(CourseVisibility.PUBLIC, owner, null))
    }

    @Test
    fun `FOLLOWER 는 소유자와 팔로워만 볼 수 있다`() {
        `when`(viewerInteractionPort.isFollowing(other, owner)).thenReturn(true)
        assertTrue(policy.isViewable(CourseVisibility.FOLLOWER, owner, owner)) // 소유자
        assertTrue(policy.isViewable(CourseVisibility.FOLLOWER, owner, other)) // 팔로워
    }

    @Test
    fun `FOLLOWER 는 미팔로우·비로그인은 볼 수 없다`() {
        `when`(viewerInteractionPort.isFollowing(other, owner)).thenReturn(false)
        assertFalse(policy.isViewable(CourseVisibility.FOLLOWER, owner, other))
        assertFalse(policy.isViewable(CourseVisibility.FOLLOWER, owner, null))
    }

    @Test
    fun `PRIVATE 은 소유자만 볼 수 있다`() {
        assertTrue(policy.isViewable(CourseVisibility.PRIVATE, owner, owner))
        assertFalse(policy.isViewable(CourseVisibility.PRIVATE, owner, other))
        assertFalse(policy.isViewable(CourseVisibility.PRIVATE, owner, null))
    }
}
