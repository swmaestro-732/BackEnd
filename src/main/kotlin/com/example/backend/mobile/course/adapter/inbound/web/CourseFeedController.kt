package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseFeedResponse
import com.example.backend.mobile.course.application.port.inbound.CourseFeedUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 공개 코스 피드 **화면 조합 API** (BFF).
 * 저장수 내림차순·최신순으로 랭킹한 공개 코스 목록을 내려준다(비로그인 포함 누구나 조회 가능).
 * 조합(공개 후보 조회 + 저장수 집계)은 인바운드 포트([CourseFeedUseCase])가 담당하고,
 * 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([CourseFeedResponse.MOCK])을 반환한다.
 */
@RestController
@RequestMapping("/service/v1")
@Validated
class CourseFeedController(
    private val courseFeedUseCase: CourseFeedUseCase,
) {
    @GetMapping("/courses")
    fun getCourseFeed(
        @RequestParam(required = false)
        @Min(1, message = "1 이상이어야 합니다")
        @Max(50, message = "50 이하여야 합니다")
        size: Int = 20,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseFeedResponse> {
        if (mock) return ApiResponse.success(CourseFeedResponse.MOCK)

        return ApiResponse.success(CourseFeedResponse.from(courseFeedUseCase.getFeed(size)))
    }
}
