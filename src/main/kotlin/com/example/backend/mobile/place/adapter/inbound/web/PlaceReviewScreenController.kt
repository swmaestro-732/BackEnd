package com.example.backend.mobile.place.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.web.SortDirection
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewListResponse
import com.example.backend.mobile.place.application.port.inbound.PlaceReviewScreenUseCase
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenQuery
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 장소 후기 전체보기 **화면 조합 API**(BFF) */
@RestController
@RequestMapping("/service/v1")
class PlaceReviewScreenController(
    private val placeReviewScreenUseCase: PlaceReviewScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/places/{placeId}/reviews")
    fun getScreen(
        @PathVariable placeId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) sort: PlaceReviewSortKey = PlaceReviewSortKey.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlaceReviewListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(PlaceReviewListResponse.mock())

        return ApiResponse.success(
            PlaceReviewListResponse.from(
                placeReviewScreenUseCase.getScreen(
                    PlaceReviewScreenQuery(
                        placeId = placeId,
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
