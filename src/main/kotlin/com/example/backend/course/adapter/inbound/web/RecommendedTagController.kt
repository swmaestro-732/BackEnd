package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.RecommendedTagsResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 코스 생성 시 추천 태그 **모킹 API** — 노션 명세(Course · 추천 태그) 기준 목업 응답.
 * 코스에 담긴 장소들(`placeIds`)을 보고 태그를 추천한다. 장소가 아직 없으면 인기 태그 fallback.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4001`).
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 */
@RestController
@RequestMapping("/api/v1/recommended-tags")
class RecommendedTagController {
    @GetMapping
    fun recommendedTags(
        @RequestParam(required = false) placeIds: List<Long>?,
        @RequestParam(required = false, defaultValue = "10")
        @Min(1, message = "1 이상이어야 합니다")
        @Max(30, message = "30 이하여야 합니다")
        limit: Int,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<RecommendedTagsResponse> {
        MockErrors.throwIfRequested(mockError)
        val tags = if (placeIds.isNullOrEmpty()) POPULAR_TAGS else PLACE_BASED_TAGS
        return ApiResponse.success(RecommendedTagsResponse(tags.take(limit)))
    }

    companion object {
        // 모킹 데이터 — placeIds가 있으면 장소(카테고리·리뷰 태그) 기반 추천을 흉내낸다(디자인 목업의 칩 예시 기준).
        private val PLACE_BASED_TAGS = listOf("감성카페", "통창뷰", "조용한", "웨이팅없음", "데이트", "비오는날")

        // 장소 담기 전 fallback — 인기 태그.
        private val POPULAR_TAGS = listOf("데이트", "맛집투어", "도심산책", "감성카페", "야경", "실내데이트")
    }
}
