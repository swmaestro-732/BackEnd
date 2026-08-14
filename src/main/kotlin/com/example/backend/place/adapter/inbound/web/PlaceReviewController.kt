package com.example.backend.place.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.request.CreatePlaceReviewRequest
import com.example.backend.place.adapter.inbound.web.response.CreatePlaceReviewResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceReviewListResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 장소 리뷰(노션 명세 · Place · place-review). **모킹 API**.
 * 노션 명세서의 두 페이지("장소 리뷰 생성"·"장소 리뷰 목록 조회") 본문이 비어 있어
 * 필드는 디자인에서 도출했다(밴드 I · 장소 리뷰 작성, 장소 상세 시트 리뷰 섹션, 후기 전체보기).
 *
 * - [create] 장소 리뷰 생성(`POST /api/v1/places/{placeId}/reviews`): 요청 형식만 검증하고 저장 없이
 *   고정 id([NEXT_REVIEW_ID])를 반환한다. 작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를
 *   받으므로 유효한 토큰이 있어야 한다(익명 요청은 401).
 * - [list] 장소 리뷰 목록 조회(`GET /api/v1/places/{placeId}/reviews`): 정렬·커서 파라미터는 계약 확인용으로
 *   받기만 하고, 항상 [PlaceReviewListResponse.mock] 을 **고정 응답**(nextCursor=null·hasNext=false)으로 내려준다.
 *   조회자별 방문 여부(`hasVisitedPlace`)도 고정값이라 `@CurrentUserId` 는 선택(비로그인 조회 허용)이다.
 *
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 쿼리 파라미터(모두 받기만 하고 응답에 영향 없음)
 * - sort: LATEST(작성일, 기본) | RATING(평점) — 디자인 "최신순 / 높은 평점".
 * - order: ASC(오름차순) | DESC(내림차순, 기본).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
 * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
 */
@RestController
@RequestMapping("/api/v1/places/{placeId}/reviews")
class PlaceReviewController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable placeId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreatePlaceReviewRequest,
    ): ApiResponse<CreatePlaceReviewResponse> =
        ApiResponse.success(CreatePlaceReviewResponse(reviewId = NEXT_REVIEW_ID), "리뷰가 등록되었습니다.")

    @GetMapping
    fun list(
        @PathVariable placeId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) sort: PlaceReviewSort = PlaceReviewSort.LATEST,
        @RequestParam(required = false) order: SortDirection = SortDirection.DESC,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<PlaceReviewListResponse> = ApiResponse.success(PlaceReviewListResponse.mock())

    private companion object {
        /** 생성 모킹 고정 id — 목 리뷰(1~6) 다음 번호. */
        const val NEXT_REVIEW_ID = 7L
    }
}
