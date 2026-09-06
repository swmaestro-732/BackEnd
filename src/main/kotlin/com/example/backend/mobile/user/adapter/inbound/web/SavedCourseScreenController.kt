package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedCourseScreenResponse
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 저장함 · 코스 탭 **화면 조합 API** (BFF) — `GET /service/v1/my/saved-courses`.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 조회 없이 고정 목([SavedCourseScreenResponse.mock])을 반환한다.
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class SavedCourseScreenController(
    private val savedCourseScreenUseCase: SavedCourseScreenUseCase,
    private val mockGuard: MockGuard,
) {
    @GetMapping("/my/saved-courses")
    fun getScreen(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) completed: Boolean?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SavedCourseScreenResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(SavedCourseScreenResponse.mock())

        return ApiResponse.success(
            SavedCourseScreenResponse.from(
                savedCourseScreenUseCase.getScreen(
                    SavedCourseScreenCommand(
                        userId = userId,
                        folderId = folderId,
                        completed = completed,
                        cursor = cursor,
                        size = size,
                    ),
                ),
            ),
        )
    }
}
