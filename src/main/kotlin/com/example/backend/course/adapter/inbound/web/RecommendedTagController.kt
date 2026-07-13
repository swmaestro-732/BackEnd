package com.example.backend.course.adapter.inbound.web

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
 */
@RestController
@RequestMapping("/api/v1/recommended-tags")
class RecommendedTagController(
    private val recommendedTagUseCase: RecommendedTagUseCase,
) {
    @GetMapping
    fun recommendedTags(
        @RequestParam(required = false) placeIds: List<Long>?,
        @RequestParam(required = false, defaultValue = "10")
        @Min(1, message = "1 이상이어야 합니다")
        @Max(30, message = "30 이하여야 합니다")
        limit: Int,
    ): ApiResponse<RecommendedTagsResponse> =
        ApiResponse.success(
            RecommendedTagsResponse(recommendedTagUseCase.recommend(placeIds.orEmpty(), limit)),
        )
}
