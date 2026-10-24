package com.example.backend.notification.adapter.outbound.lookup

import com.example.backend.notification.application.port.outbound.NotificationActor
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
    fun `행위자 프로필에서 닉네임과 프로필 이미지를 반환한다`() {
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

        assertThat(adapter.findActor(7L)).isEqualTo(NotificationActor("산책자", "https://example.com/profile.jpg"))
        verify(users).getProfiles(listOf(7L), null)
    }

    @Test
    fun `행위자가 조회 결과에 없으면 null을 반환한다`() {
        `when`(users.getProfiles(listOf(7L), null)).thenReturn(emptyList())

        assertThat(adapter.findActor(7L)).isNull()
        verify(users).getProfiles(listOf(7L), null)
    }
}
