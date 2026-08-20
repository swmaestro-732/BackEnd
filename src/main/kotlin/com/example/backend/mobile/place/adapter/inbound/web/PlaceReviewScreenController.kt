package com.example.backend.mobile.place.adapter.inbound.web

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

/**
 * 장소 후기 전체보기 **화면 조합 목업 API** (BFF) — `GET /service/v1/places/{placeId}/reviews`.
 * 리뷰(place) + **작성자 프로필**(user)을 병합해 내려주므로 도메인 API 가 아니라 service 경로에 둔다
 * (api-design.md: 도메인 API 는 해당 도메인만, 화면 조합은 mobile BFF). 리뷰 작성·삭제는 도메인
 * ([com.example.backend.place.adapter.inbound.web.PlaceReviewController])가 그대로 담당한다.
 * 장소 상세 시트([PlaceDetailScreenController])의 리뷰 미리보기와 같은 데이터의 전체 목록 화면이다.
 *
 * 항상 [PlaceReviewListResponse.mock] 을 **고정 응답**(nextCursor=null·hasNext=false)으로 내려준다 —
 * 정렬·커서 파라미터는 API 계약 확인용으로 받기만 하고 동작은 실구현에서 지원한다.
 * 조회자별 방문 여부(`hasVisitedPlace`)도 고정값이라 `@CurrentUserId` 는 선택(비로그인 조회 허용)이다.
 * 실제 구현 시 place + user 인바운드 포트 조합으로 교체한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class PlaceReviewScreenController {
    /**
     * 쿼리 파라미터(모두 받기만 하고 응답에 영향 없음)
     * - sort: LATEST(작성일, 기본) | RATING(평점) — 디자인 "최신순 / 높은 평점".
     * - order: ASC(오름차순) | DESC(내림차순, 기본).
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
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
