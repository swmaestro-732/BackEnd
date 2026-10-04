package com.example.backend.response

import com.example.backend.common.response.DirectionErrorCode
import com.example.backend.course.adapter.inbound.web.response.CreateCourseReviewResponse
import com.example.backend.direction.adapter.inbound.web.response.WalkingResponse
import com.example.backend.user.adapter.inbound.web.response.AccountProfileResponse
import com.example.backend.user.adapter.inbound.web.response.UserAreaResponse
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.inbound.dto.UserAreaResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 커버되지 않은 응답 DTO companion / 에러 코드 경로를 최소 비용으로 검증한다.
 */
class ResponseDtoTest {
    @Test
    fun `WalkingResponse - from 은 segments 를 그대로 래핑한다`() {
        val response = WalkingResponse.from(listOf(10, -1, 8))

        assertEquals(listOf(10, -1, 8), response.segments)
    }

    @Test
    fun `WalkingResponse - 빈 segments 도 허용된다`() {
        val response = WalkingResponse(segments = emptyList())

        assertEquals(emptyList<Int>(), response.segments)
    }

    @Test
    fun `DirectionErrorCode - 속성 값이 스펙에 맞는다`() {
        val code = DirectionErrorCode.DIRECTION_UNAVAILABLE

        assertEquals(503, code.status)
        assertEquals(5030, code.code)
        assertEquals("도보 경로 서비스를 일시적으로 이용할 수 없습니다.", code.message)
    }

    @Test
    fun `UserAreaResponse - from 은 code 와 name 을 올바르게 매핑한다`() {
        val result = UserAreaResult(code = "1168010100", name = "역삼동")

        val response = UserAreaResponse.from(result)

        assertEquals("1168010100", response.code)
        assertEquals("역삼동", response.name)
    }

    @Test
    fun `AccountProfileResponse - from 은 UserProfileResult 를 응답으로 변환한다`() {
        val profileResult =
            UserProfileResult(
                id = 1L,
                nickname = "테스터",
                handle = "tester",
                profileImageUrl = null,
                bio = null,
                isFollowing = false,
                isFollower = false,
                followersCnt = 0,
                followingsCnt = 0,
                publicCoursesCnt = 0,
                followerCoursesCnt = 0,
                privateCoursesCnt = 0,
            )

        val response = AccountProfileResponse.from(profileResult)

        assertEquals(1L, response.id)
        assertEquals("테스터", response.nickname)
        assertEquals("tester", response.handle)
        assertNull(response.profileImageUrl)
        assertNull(response.bio)
        assertEquals(emptyList<String>(), response.likeThemes)
        assertEquals(emptyList<UserAreaResponse>(), response.areas)
    }

    @Test
    fun `CreateCourseReviewResponse - reviewId 를 저장한다`() {
        val response = CreateCourseReviewResponse(reviewId = 42L)

        assertEquals(42L, response.reviewId)
    }

    @Test
    fun `UserSummaryUseCase UserSummary - 필드를 올바르게 저장한다`() {
        val summary = UserSummaryUseCase.UserSummary(id = 1L, nickname = "테스터", profileImageUrl = null)

        assertEquals(1L, summary.id)
        assertEquals("테스터", summary.nickname)
        assertNull(summary.profileImageUrl)
    }
}
