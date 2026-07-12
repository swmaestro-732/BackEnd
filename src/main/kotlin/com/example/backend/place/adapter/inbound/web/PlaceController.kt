package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceDetailResponse
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 장소 상세(노션 명세 · Place · 장소 상세).
 * Result → Response 매핑만 담당하고 조합·정책은 [PlaceQueryUseCase] 구현이 가진다.
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeQueryUseCase: PlaceQueryUseCase,
) {
    @GetMapping("/{placeId}")
    fun detail(
        @PathVariable placeId: Long,
    ): ApiResponse<PlaceDetailResponse> =
        ApiResponse.success(PlaceDetailResponse.from(placeQueryUseCase.getDetail(placeId)))
}
