package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.web.SortDirection
import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseReviewListResponse
import com.example.backend.mobile.course.application.port.inbound.CourseReviewScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenQuery
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 코스 후기 전체보기 **화면 조합 API**(BFF) */
@RestController
@RequestMapping("/service/v1")
class CourseReviewScreenController(
    private val courseReviewScreenUseCase: CourseReviewScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/courses/{courseId}/reviews")
    fun getScreen(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) sort: CourseReviewSortKey = CourseReviewSortKey.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseReviewListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseReviewListResponse.mock())

        return ApiResponse.success(
            CourseReviewListResponse.from(
                courseReviewScreenUseCase.getScreen(
                    CourseReviewScreenQuery(
                        courseId = courseId,
                        sort = sort,
                        descending = order == SortDirection.DESC,
                        cursor = cursor,
                        size = size,
                    ),
                ),
            ),
        )
    }
}
