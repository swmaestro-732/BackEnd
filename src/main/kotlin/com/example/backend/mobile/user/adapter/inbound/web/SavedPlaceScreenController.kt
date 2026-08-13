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
 * 저장 레코드·배지 카운트(user) + 장소 요약(place) + 지역 이름(area)을 한 번에 내려준다.
 *
 * 조합은 인바운드 포트([SavedPlaceScreenUseCase])가 담당하고, 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 * "내" 저장함이라 userId 가 필수이며, `/service/v1/my` 하위는 SecurityConfig 가 JWT 인증을 강제한다
 * (`@CurrentUserId` 는 항상 non-null). 목업 시절 경로(`GET /service/v1/places/save`)에서 옮겼다 —
 * 저장함은 "내" 리소스라 인증이 경로로 보장되는 `/my` 하위가 맞다(저장함 코스 탭 선례와 대칭).
 *
 * 거리(walkingTime)·거리 정렬은 1차 구현 범위 밖이다 — userLat/userLng 는 API 계약 유지를 위해 받기만 하고,
 * 응답 walkingTime 은 항상 null 이며 정렬은 최신 저장순이다(별도 티켓에서 지원).
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
    /**
     * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준.
     * - category: 저장 카테고리 칩 필터. 저장 카테고리 이름(예: CAFE) — 잘못된 값은 400(도메인 서비스가 검증).
     *   BFF 는 user 도메인 enum 을 참조할 수 없어 문자열로 받는다(도메인 조회는 enum 으로 바인딩).
     * - userLat/userLng: 사용자 현재 위치(선택) — 거리 지원 시 쓴다. 범위 밖은 400.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 형식이 잘못되면 400.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
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
