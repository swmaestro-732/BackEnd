package com.example.backend.place.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.adapter.inbound.web.response.PlaceSearchResponse
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 장소(노션 명세 · Place).
 *
 * - [search] 장소 검색(`GET /api/v1/places`): **실구현** — 인바운드 포트([PlaceQueryUseCase])로 검색한다.
 *   OpenSearch 우선(지역·카테고리 토큰 해석 + 텍스트 검색), 미가용·실패 시 DB LIKE 폴백. 커서는 불투명
 *   토큰이다(`cursor`/`size` ↔ `nextCursor`/`hasNext` — 형식은 서비스 내부, 클라이언트는 해석하지 않는다).
 *   시드 데이터가 없는 개발 환경을 위해 `?mock=true` 고정 응답([PlaceSearchResponse.mock]) 폴백을 유지한다.
 *   모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 실구현은 `q`(이름 검색)와 커서 페이지네이션만 반영한다 — 뷰포트(sw/ne)·category·sort 는 **후속 과제**로,
 * 파라미터는 받되 아직 적용하지 않는다(모킹 폴백은 고정 응답이라 파라미터를 무시한다).
 *
 * 장소 상세는 화면 조합이라 BFF 경로로 이관했다 → `GET /service/v1/places/{placeId}`
 * ([com.example.backend.mobile.place.adapter.inbound.web.PlaceDetailScreenController]).
 */
@Tag(name = "Place")
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
//        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") swLat: Double?,
//        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") swLng: Double?,
//        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") neLat: Double?,
//        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") neLng: Double?,
//        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") userLat: Double?,
//        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") userLng: Double?,
//        @RequestParam(required = false) category: String?,
//        @RequestParam(required = false) sort: PlaceSearchSort = PlaceSearchSort.DISTANCE,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlaceSearchResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(PlaceSearchResponse.mock())
        }

        return ApiResponse.success(
            PlaceSearchResponse.from(
                placeQueryUseCase.searchByName(q.orEmpty(), cursor, size),
            ),
        )
    }
}
