package com.example.backend.place.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceMapResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceSearchResponse
import com.example.backend.place.application.port.inbound.PlaceMapQueryUseCase
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 장소 검색 — 코스 생성용 목록과 지도용 마커·클러스터 조회. */
@Tag(name = "Place")
@RequiresAppFeature(AppFeature.PLACE_SEARCH)
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val placeMapQueryUseCase: PlaceMapQueryUseCase,
    private val mockGuard: MockGuard,
) {
    /** 코스 생성용 목록. 기준 위치는 정렬 가산점으로만 사용한다. */
    @GetMapping
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) @Min(1) anchorPlaceId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<PlaceSearchResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(PlaceSearchResponse.mock())
        }

        return ApiResponse.success(
            PlaceSearchResponse.from(
                placeQueryUseCase.searchByName(q.orEmpty(), cursor, size, anchorPlaceId),
            ),
        )
    }

    /**
     * 지도 검색. 필수 뷰포트 안의 마커·클러스터를 반환한다.
     * `sort=DISTANCE` 는 `userLat/userLng`(둘 다 또는 둘 다 없음, 아니면 400) 기준, 없으면 뷰포트 중심 기준이다.
     */
    @GetMapping("/map")
    fun searchMap(
        @RequestParam swLat: Double,
        @RequestParam swLng: Double,
        @RequestParam neLat: Double,
        @RequestParam neLng: Double,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) category: PlaceMapCategory?,
        @RequestParam(required = false) sort: PlaceMapSort = PlaceMapSort.RELEVANCE,
        @RequestParam(required = false) userLat: Double?,
        @RequestParam(required = false) userLng: Double?,
    ): ApiResponse<PlaceMapResponse> {
        require((userLat == null) == (userLng == null)) { "userLat 과 userLng 는 함께 보내야 합니다." }
        val userLocation = userLat?.let { Coordinate(it, userLng!!) }
        return ApiResponse.success(
            PlaceMapResponse.from(
                placeMapQueryUseCase.searchMap(
                    q.orEmpty(),
                    Viewport(Coordinate(swLat, swLng), Coordinate(neLat, neLng)),
                    category?.name,
                    sort,
                    userLocation,
                ),
            ),
        )
    }
}
