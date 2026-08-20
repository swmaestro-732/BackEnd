package com.example.backend.place.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.request.CreatePlaceReviewRequest
import com.example.backend.place.adapter.inbound.web.response.CreatePlaceReviewResponse
import com.example.backend.place.application.port.inbound.PlaceReviewUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 장소 리뷰 작성·삭제(노션 명세 · Place · place-review).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 생성은 `?mock=true` 면 저장 없이 고정 id([CreatePlaceReviewResponse.MOCK])를
 * 반환한다(장소 저장 선례와 동일 규칙 — 운영 프로파일에서는 [MockGuard] 가 무시한다).
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/api/v1/places/{placeId}/reviews")
class PlaceReviewController(
    private val placeReviewUseCase: PlaceReviewUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun create(
        @PathVariable placeId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreatePlaceReviewRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CreatePlaceReviewResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(CreatePlaceReviewResponse.MOCK, "리뷰가 등록되었습니다.")
        }

        val review = placeReviewUseCase.create(request.toCommand(placeId, userId))
        return ApiResponse.success(CreatePlaceReviewResponse.from(review), "리뷰가 등록되었습니다.")
    }

    @DeleteMapping("/{reviewId}")
    fun delete(
        @PathVariable placeId: Long,
        @PathVariable reviewId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("리뷰가 삭제되었습니다.")
}
