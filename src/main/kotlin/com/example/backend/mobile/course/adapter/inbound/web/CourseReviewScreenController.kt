package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseReviewListResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 코스 후기 전체보기 **화면 조합 목업 API** (BFF)  */
@RequiresAppFeature("course-review")
@RestController
@RequestMapping("/service/v1")
class CourseReviewScreenController {
    /**
     * 쿼리 파라미터(모두 받기만 하고 응답에 영향 없음)
     * - sort: LATEST(작성일, 기본) | RATING(평점) — 디자인 "최신순 / 높은 평점".
     * - order: ASC(오름차순) | DESC(내림차순, 기본).
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/courses/{courseId}/reviews")
    fun getScreen(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) sort: CourseReviewSort = CourseReviewSort.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<CourseReviewListResponse> = ApiResponse.success(CourseReviewListResponse.mock())
}
