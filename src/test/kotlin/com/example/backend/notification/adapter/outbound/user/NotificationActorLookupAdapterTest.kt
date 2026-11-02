package com.example.backend.notification.adapter.outbound.user

import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NotificationActorLookupAdapterTest {
    private val users = mock(UserUseCase::class.java)
    private val adapter = NotificationActorLookupAdapter(users)

    @Test
    fun `행위자 프로필에서 닉네임을 반환한다`() {
        val profile =
            UserProfileResult(
                id = 7L,
                nickname = "산책자",
                handle = "walker",
                profileImageUrl = "https://example.com/profile.jpg",
                bio = null,
                isFollowing = false,
                isFollower = false,
                followersCnt = 0,
                followingsCnt = 0,
                publicCoursesCnt = 0,
                followerCoursesCnt = 0,
                privateCoursesCnt = 0,
            )
        `when`(users.getProfiles(listOf(7L), null)).thenReturn(listOf(profile))

        assertThat(adapter.findNickname(7L)).isEqualTo("산책자")
        verify(users).getProfiles(listOf(7L), null)
    }

    @Test
    fun `행위자가 조회 결과에 없으면 null을 반환한다`() {
        `when`(users.getProfiles(listOf(7L), null)).thenReturn(emptyList())

        assertThat(adapter.findNickname(7L)).isNull()
        verify(users).getProfiles(listOf(7L), null)
    }

    @Test
    fun `잠금은 user 유스케이스의 활성 사용자 잠금에 위임한다`() {
        `when`(users.lockActive(listOf(7L, 8L))).thenReturn(setOf(7L))

        assertThat(adapter.lockActive(listOf(7L, 8L))).containsExactly(7L)
    }
}
