package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.RecommendedTagsResponse
import com.example.backend.course.application.port.inbound.RecommendedTagUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 생성 시 추천 태그(노션 명세 · Course · 추천 태그).
 * 코스에 담긴 장소들(`placeIds`)을 기반으로 추천하고, 장소가 없으면 인기 태그 fallback.
 *
 * `?mock=true` 는 DB에 데이터가 없는 동안 프론트 개발용 **모킹 폴백** — 고정 태그를 반환한다.
 * 시드 데이터가 확보되면 파라미터와 모킹 상수를 제거한다.
 */
@RestController
@RequestMapping("/api/v1/recommended-tags")
class RecommendedTagController(
    private val recommendedTagUseCase: RecommendedTagUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping
    fun recommendedTags(
        @RequestParam(required = false) placeIds: List<Long>?,
        @RequestParam(required = false, defaultValue = "10")
        @Min(1, message = "1 이상이어야 합니다")
        @Max(30, message = "30 이하여야 합니다")
        limit: Int,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<RecommendedTagsResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            val tags = if (placeIds.isNullOrEmpty()) POPULAR_TAGS else PLACE_BASED_TAGS
            return ApiResponse.success(RecommendedTagsResponse(tags.take(limit)))
        }
        return ApiResponse.success(
            RecommendedTagsResponse(recommendedTagUseCase.recommend(placeIds.orEmpty(), limit)),
        )
    }

    companion object {
        // 모킹 폴백 데이터(#15와 동일) — placeIds가 있으면 장소 기반 추천을 흉내낸다(디자인 목업의 칩 예시 기준).
        private val PLACE_BASED_TAGS = listOf("감성카페", "통창뷰", "조용한", "웨이팅없음", "데이트", "비오는날")

        // 장소 담기 전 fallback — 인기 태그.
        private val POPULAR_TAGS = listOf("데이트", "맛집투어", "도심산책", "감성카페", "야경", "실내데이트")
    }
}
