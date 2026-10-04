package com.example.backend.mobile.home.adapter.inbound.web.v2.response

import com.example.backend.mobile.home.application.port.inbound.dto.HomeFeedResult

/**
 * 웹 응답 DTO — 추천 코스 전체보기(공개 코스 피드 커서 페이지). 카드는 홈 추천 코스와 같은 [HomeResponse.Course] 다.
 */
data class RecommendedCoursesResponse(
    val courses: List<HomeResponse.Course>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: HomeFeedResult): RecommendedCoursesResponse =
            RecommendedCoursesResponse(
                courses = result.courses.map(HomeResponse.Course::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}
