package com.example.backend.area.adapter.inbound.web

import com.example.backend.area.adapter.inbound.web.response.AreaViewResponse
import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 행정구역(Area) 조회 API.
 *
 * - `GET /api/v1/areas/search?keyword=강남` 시군구+읍면동 통합 검색(상위 20건).
 *   keyword 가 비면 빈 목록을 내려준다.
 */
@RestController
@RequestMapping("/api/v1/areas")
class AreaController(
    private val areaQueryUseCase: AreaQueryUseCase,
) {
    @GetMapping("/search")
    fun searchAreas(
        @RequestParam(required = false) keyword: String?,
    ): ApiResponse<List<AreaViewResponse>> =
        ApiResponse.success(AreaViewResponse.from(areaQueryUseCase.searchAreas(keyword.orEmpty())))
}
