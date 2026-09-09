package com.example.backend.mobile.place.adapter.inbound.web

import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewListResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 장소 후기 전체보기 **화면 조합 목업 API** (BFF) */
@RequiresAppFeature("place-review")
@RestController
@RequestMapping("/service/v1")
class PlaceReviewScreenController {
    @GetMapping("/places/{placeId}/reviews")
    fun getScreen(
        @PathVariable placeId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) sort: PlaceReviewSort = PlaceReviewSort.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<PlaceReviewListResponse> = ApiResponse.success(PlaceReviewListResponse.mock())
}
