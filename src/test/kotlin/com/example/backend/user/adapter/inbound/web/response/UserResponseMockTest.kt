package com.example.backend.user.adapter.inbound.web.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SavedPlaceListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = SavedPlaceListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 savedPlaces 개수와 일치한다`() {
        val response = SavedPlaceListResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.savedPlaces.size)
    }

    @Test
    fun `mock() visitedCount + unvisitedCount 는 totalCount 와 일치한다`() {
        val response = SavedPlaceListResponse.mock()

        assertThat(response.visitedCount + response.unvisitedCount).isEqualTo(response.totalCount)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = SavedPlaceListResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() categoryCounts 는 비어 있지 않다`() {
        val response = SavedPlaceListResponse.mock()

        assertThat(response.categoryCounts).isNotEmpty
    }
}

class AccountProfileResponseMockTest {
    @Test
    fun `mock() 인자 없이 호출하면 고정 프로필을 반환한다`() {
        val response = AccountProfileResponse.mock()

        assertThat(response.id).isPositive
        assertThat(response.nickname).isNotBlank
    }

    @Test
    fun `mock() nickname 인자를 넘기면 해당 값이 적용된다`() {
        val response = AccountProfileResponse.mock(nickname = "테스트유저")

        assertThat(response.nickname).isEqualTo("테스트유저")
    }

    @Test
    fun `mock() handle 인자를 넘기면 해당 값이 적용된다`() {
        val response = AccountProfileResponse.mock(handle = "test_handle")

        assertThat(response.handle).isEqualTo("test_handle")
    }

    @Test
    fun `mock() likeThemes 와 areas 는 비어 있지 않다`() {
        val response = AccountProfileResponse.mock()

        assertThat(response.likeThemes).isNotEmpty
        assertThat(response.areas).isNotEmpty
    }
}

class SavedCourseListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = SavedCourseListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 savedCourses 개수와 일치한다`() {
        val response = SavedCourseListResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.savedCourses.size)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = SavedCourseListResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 저장 코스는 id·courseId·savedAt 이 채워져 있다`() {
        val response = SavedCourseListResponse.mock()

        response.savedCourses.forEach { item ->
            assertThat(item.id).isPositive
            assertThat(item.courseId).isPositive
            assertThat(item.savedAt).isNotNull
        }
    }
}

class FollowListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = FollowListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() totalCount 는 users 개수와 일치한다`() {
        val response = FollowListResponse.mock()

        assertThat(response.totalCount).isEqualTo(response.users.size)
    }

    @Test
    fun `mock() hasNext 는 false 이고 nextCursor 는 null 이다`() {
        val response = FollowListResponse.mock()

        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
    }

    @Test
    fun `mock() 각 유저는 id·nickname 이 채워져 있다`() {
        val response = FollowListResponse.mock()

        response.users.forEach { user ->
            assertThat(user.id).isPositive
            assertThat(user.nickname).isNotBlank
        }
    }
}

class CourseFolderListResponseMockTest {
    @Test
    fun `mock() 은 non-null 응답을 반환한다`() {
        val response = CourseFolderListResponse.mock()

        assertThat(response).isNotNull
    }

    @Test
    fun `mock() folderCount 는 folders 개수와 일치한다`() {
        val response = CourseFolderListResponse.mock()

        assertThat(response.folderCount).isEqualTo(response.folders.size)
    }

    @Test
    fun `mock() folders 목록은 비어 있지 않다`() {
        val response = CourseFolderListResponse.mock()

        assertThat(response.folders).isNotEmpty
    }

    @Test
    fun `mock() 각 폴더는 id·name 이 채워져 있다`() {
        val response = CourseFolderListResponse.mock()

        response.folders.forEach { folder ->
            assertThat(folder.id).isPositive
            assertThat(folder.name).isNotBlank
        }
    }
}

class AuthResponsesMockTest {
    @Test
    fun `SocialLoginResponse mock() 은 accessToken 이 적용된다`() {
        val response = SocialLoginResponse.mock(accessToken = "test-token")

        assertThat(response.accessToken).isEqualTo("test-token")
        assertThat(response.refreshToken).isNotBlank
        assertThat(response.isNewUser).isFalse
    }

    @Test
    fun `TokenResponse mock() 은 accessToken 이 적용된다`() {
        val response = TokenResponse.mock(accessToken = "refresh-test")

        assertThat(response.accessToken).isEqualTo("refresh-test")
        assertThat(response.refreshToken).isNotBlank
    }

    @Test
    fun `SignupResponse mock() 은 user 정보가 채워진다`() {
        val response =
            SignupResponse.mock(
                accessToken = "at",
                nickname = "닉",
                handle = "handle",
                profileImageUrl = null,
            )

        assertThat(response.accessToken).isEqualTo("at")
        assertThat(response.user.nickname).isEqualTo("닉")
        assertThat(response.user.handle).isEqualTo("handle")
        assertThat(response.user.id).isPositive
    }
}

class FollowResponseMockTest {
    @Test
    fun `mock(isFollowing=true) 팔로우 상태면 followersCnt 가 양수다`() {
        val response = FollowResponse.mock(isFollowing = true)

        assertThat(response.isFollowing).isTrue
        assertThat(response.followersCnt).isPositive
    }

    @Test
    fun `mock(isFollowing=false) 언팔로우 상태면 isFollowing 이 false 다`() {
        val response = FollowResponse.mock(isFollowing = false)

        assertThat(response.isFollowing).isFalse
        assertThat(response.followersCnt).isNotNegative
    }
}

class CreateCourseFolderResponseMockTest {
    @Test
    fun `mock() 은 양수 folderId 를 반환한다`() {
        val response = CreateCourseFolderResponse.mock()

        assertThat(response.folderId).isPositive
    }
}
