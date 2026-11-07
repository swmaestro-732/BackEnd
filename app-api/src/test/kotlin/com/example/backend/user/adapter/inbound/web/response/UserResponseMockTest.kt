package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.SavedPlacesResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

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

    @Test
    fun `from() — SavedPlacesResult 를 SavedPlaceListResponse 로 변환한다`() {
        val now = Instant.parse("2026-08-01T00:00:00Z")
        val result =
            SavedPlacesResult(
                totalCount = 2L,
                unvisitedCount = 1L,
                visitedCount = 1L,
                categoryCounts = listOf(SavedPlacesResult.CategoryCount(category = "CAFE", count = 2L)),
                nextCursor = "cursor-abc",
                hasNext = true,
                savedPlaces =
                    listOf(
                        SavedPlacesResult.SavedPlaceItem(
                            id = 10L,
                            placeId = 101L,
                            category = "CAFE",
                            visited = false,
                            savedAt = now,
                        ),
                        SavedPlacesResult.SavedPlaceItem(
                            id = 11L,
                            placeId = 102L,
                            category = null,
                            visited = true,
                            savedAt = now,
                        ),
                    ),
            )

        val response = SavedPlaceListResponse.from(result)

        assertThat(response.totalCount).isEqualTo(2)
        assertThat(response.unvisitedCount).isEqualTo(1)
        assertThat(response.visitedCount).isEqualTo(1)
        assertThat(response.categoryCounts).hasSize(1)
        assertThat(response.categoryCounts[0].category).isEqualTo("CAFE")
        assertThat(response.categoryCounts[0].count).isEqualTo(2)
        assertThat(response.nextCursor).isEqualTo("cursor-abc")
        assertThat(response.hasNext).isTrue
        assertThat(response.savedPlaces).hasSize(2)
        assertThat(response.savedPlaces[0].placeId).isEqualTo(101L)
        assertThat(response.savedPlaces[1].category).isNull()
    }

    @Test
    fun `from() — 빈 결과를 빈 응답으로 변환한다`() {
        val result =
            SavedPlacesResult(
                totalCount = 0L,
                unvisitedCount = 0L,
                visitedCount = 0L,
                categoryCounts = emptyList(),
                nextCursor = null,
                hasNext = false,
                savedPlaces = emptyList(),
            )

        val response = SavedPlaceListResponse.from(result)

        assertThat(response.totalCount).isEqualTo(0)
        assertThat(response.hasNext).isFalse
        assertThat(response.nextCursor).isNull()
        assertThat(response.savedPlaces).isEmpty()
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

        assertThat(response.savedCourses).isNotEmpty
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

        assertThat(response.users).isNotEmpty
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

        assertThat(response.folders).isNotEmpty
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
