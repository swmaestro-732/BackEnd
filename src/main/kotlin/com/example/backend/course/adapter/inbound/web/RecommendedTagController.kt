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
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4001`).
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 */
@RestController
@RequestMapping("/api/v1/recommended-tags")
class RecommendedTagController {
    @GetMapping
    fun recommendedTags(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "10")
        @Min(1, message = "1 이상이어야 합니다")
        @Max(30, message = "30 이하여야 합니다")
        limit: Int,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<RecommendedTagsResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(RecommendedTagsResponse(MOCK_TAGS.take(limit)))
    }

    companion object {
        // 모킹 데이터 — keyword와 무관하게 고정 목록을 내려준다(디자인 목업의 칩 예시 기준).
        private val MOCK_TAGS = listOf("감성카페", "통창뷰", "조용한", "웨이팅없음", "데이트", "비오는날")
    }
}
