package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseReviewListResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 리뷰 목록 조회. **모킹 API**.
 * 쿼리 파라미터는 API 계약 확인용으로 받기만 하고, 정렬·커서·페이지네이션 동작 없이
 * 항상 [CourseReviewListResponse.mock] 을 **고정 응답**(nextCursor=null·hasNext=false)으로 내려준다.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 쿼리 파라미터(모두 받기만 하고 응답에 영향 없음)
 * - sort: LATEST(작성일, 기본) | RATING(평점).
 * - order: ASC(오름차순, 기본) | DESC(내림차순).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
 * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
 */
@RestController
@RequestMapping("/api/v1")
class CourseReviewController {
    @GetMapping("/courses/{courseId}/reviews")
    fun getReviews(
        @PathVariable courseId: Long,
        @RequestParam(required = false) sort: CourseReviewSort = CourseReviewSort.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<CourseReviewListResponse> = ApiResponse.success(CourseReviewListResponse.mock())
}
