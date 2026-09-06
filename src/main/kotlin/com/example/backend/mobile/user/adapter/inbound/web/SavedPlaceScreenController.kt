package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedPlaceScreenResponse
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedPlaceScreenUseCase
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 저장함 · 장소 탭 **화면 조합 API** (BFF) — `GET /service/v1/my/saved-places`.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([SavedPlaceScreenResponse.mock])을 반환한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class SavedPlaceScreenController(
    private val savedPlaceScreenUseCase: SavedPlaceScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/my/saved-places")
    fun getScreen(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") userLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") userLng: Double?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SavedPlaceScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(SavedPlaceScreenResponse.mock())

        return ApiResponse.success(
            SavedPlaceScreenResponse.from(
                savedPlaceScreenUseCase.getScreen(
                    SavedPlaceScreenCommand(
                        userId = userId,
                        visited = visited,
                        category = category,
                        cursor = cursor,
                        size = size,
                    ),
                ),
            ),
        )
    }
}
