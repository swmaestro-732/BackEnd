package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.response.ExternalPlaceSearchResponse
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 외부 지도 장소 검색(`GET /api/v1/places/search`).
 *
 * 목 검색(`GET /api/v1/places`, [PlaceController])과 구분되는 **실구현** 엔드포인트로,
 * 카카오 로컬 키워드 검색 결과를 내려준다. lat/lng 가 둘 다 오면 근처 검색(radius)으로 사용한다.
 */
@Tag(name = "Place")
@RestController
@RequestMapping("/api/v1/places")
@Validated
class PlaceSearchController(
    private val placeSearchExternalUseCase: PlaceSearchExternalUseCase,
) {
    @GetMapping("/search")
    fun search(
        @RequestParam @NotBlank query: String,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
    ): ApiResponse<ExternalPlaceSearchResponse> {
        val near = if (lat != null && lng != null) Coordinate(latitude = lat, longitude = lng) else null
        val places = placeSearchExternalUseCase.search(query, near)
        return ApiResponse.success(ExternalPlaceSearchResponse.from(places))
    }
}
