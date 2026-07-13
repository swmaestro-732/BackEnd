package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.response.CourseReviewListResponse
import com.example.backend.course.application.dto.CourseReviewQuery
import com.example.backend.course.application.dto.CourseReviewSort
import com.example.backend.course.application.dto.SortDirection
import com.example.backend.course.application.port.inbound.CourseReviewUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 리뷰 목록 조회. HTTP 요청을 인바운드 포트([CourseReviewUseCase]) 호출로 변환한다.
 *
 * 쿼리 파라미터
 * - sort: LATEST(작성일, 기본) | RATING(평점). 잘못된 값은 400.
 * - order: DESC(내림차순, 기본) | ASC(오름차순). 잘못된 값은 400.
 *   · LATEST+DESC=최신순, LATEST+ASC=오래된순, RATING+DESC=별점 높은순, RATING+ASC=별점 낮은순.
 * - cursor: 직전 응답의 nextCursor 를 그대로 넘긴다(첫 페이지는 생략). 유효하지 않은 커서는 400.
 * - size: 페이지 크기(기본 10, 최대 50). 범위를 벗어나면 400.
 * - mockError: 모킹 에러 주입(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseReviewController(
    private val courseReviewUseCase: CourseReviewUseCase,
) {
    @GetMapping("/courses/{courseId}/reviews")
    fun getReviews(
        @PathVariable courseId: Long,
        @RequestParam(defaultValue = "LATEST") sort: CourseReviewSort,
        @RequestParam(defaultValue = "DESC") order: SortDirection,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseReviewListResponse> {
        MockErrors.throwIfRequested(mockError)
        val query = CourseReviewQuery(sort = sort, order = order, cursor = cursor, size = size)
        return ApiResponse.success(CourseReviewListResponse.from(courseReviewUseCase.getReviews(courseId, query)))
    }
}
