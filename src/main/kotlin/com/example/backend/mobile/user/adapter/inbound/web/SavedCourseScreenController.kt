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
 * 저장 레코드(user) + 코스 요약(course) + 작성자 핸들(user) + 완주 여부(trace) + 폴더 칩을 한 번에 내려준다.
 *
 * 조합(도메인 인바운드 포트 5개)은 인바운드 포트([SavedCourseScreenUseCase])가 담당하고,
 * 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다. "내" 저장함이라 userId 가 필수이며,
 * `/service/v1/my` 하위는 SecurityConfig 가 JWT 인증을 강제한다(`@CurrentUserId` 는 항상 non-null).
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
    /**
     * - folderId: 폴더 칩 필터. 생략 시 전체.
     * - completed: 완주 여부 필터(안 가봄/완주 칩). 생략 시 전체, true=완주만, false=안 가봄만.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략).
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
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
