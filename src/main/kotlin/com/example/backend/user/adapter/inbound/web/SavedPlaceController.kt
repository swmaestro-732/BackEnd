package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.SavePlaceRequest
import com.example.backend.user.adapter.inbound.web.response.SavedPlaceListResponse
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.SavedPlacesCommand
import com.example.backend.user.domain.model.SavedPlaceCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 저장 장소(노션 명세 · User · user-place).
 *
 * 저장(POST)과 조회(GET)의 경로가 다르다(명세 기준) — 저장은 장소 도메인 액션이라 `/api/v1/places/save`,
 * 조회는 "내" 저장함이라 `/api/v1/my/saved-places`. 클래스 레벨 매핑 대신 메서드 레벨 전체 경로로 둔다
 * (코스 저장 `/api/v1/courses/save` 선례 — 2026-07-28 경로 이동 결정에 따라 모킹 시절
 * `POST /api/v1/my/saved-places/{placeId}` 에서 옮기고 placeId 는 요청 바디로 받는다).
 *
 * - [save] 장소 저장(`POST /api/v1/places/save`): **실구현** — 인바운드 포트([SavedPlaceUseCase])로 저장한다.
 *   장소 존재를 검증하고(그 외 404), 이미 저장한 장소면 중복 저장으로 막는다(409). 장소당 저장 레코드는 1개다.
 *   저장 주체 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받으므로 유효한 토큰이 필요하다.
 * - [unsave] 장소 저장 취소(`DELETE /api/v1/places/save/{placeId}`): **실구현** — 저장의 역연산이라 경로를 대칭으로 두고
 *   placeId 로 (user, place) 저장 레코드를 지운다. 저장돼 있지 않아도 오류 없이 성공한다(멱등 — 코스 저장 취소 선례와 동일).
 * - [list] 저장 장소 조회(`GET /api/v1/my/saved-places`): **실구현** — 인바운드 포트([SavedPlaceUseCase])로 조회한다.
 *   `/api/v1/my` 하위라 SecurityConfig 가 JWT 인증을 강제한다. 저장 레코드(ID 위주)를 최신 저장순으로
 *   커서 페이지네이션해 반환한다 — 장소 상세(이름·평점·거리 등)는 화면 조합 API(`GET /service/v1/my/saved-places`)가 담당한다.
 * - [visit] 저장 장소 방문 처리(`PATCH /api/v1/my/saved-places/{savedPlaceId}`): **모킹 API**(실구현은 별도 티켓).
 *   경로 변수는 장소 id가 아니라 **저장 레코드 id**([SavedPlaceListResponse.SavedPlaceItem.id])다.
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 저장/조회 없이 고정 목([SavedPlaceListResponse.mock])을
 * 반환한다(코스 저장 선례와 동일 규칙). 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
class SavedPlaceController(
    private val savedPlaceUseCase: SavedPlaceUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/places/save")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: SavePlaceRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("장소가 저장되었습니다.")

        savedPlaceUseCase.save(userId, request.placeId)
        return ApiResponse.ok("장소가 저장되었습니다.")
    }

    @DeleteMapping("/api/v1/places/save/{placeId}")
    fun unsave(
        @CurrentUserId userId: Long,
        @PathVariable placeId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("장소 저장을 취소했습니다.")

        savedPlaceUseCase.unsave(userId, placeId)
        return ApiResponse.ok("장소 저장을 취소했습니다.")
    }

    /**
     * 저장 장소 방문 처리(모킹). 미방문 탭 카드 스와이프 → "방문한 곳으로 표시할까요?" 확인 후 호출 —
     * 방문 탭으로 옮기고 지도에 방문 표시를 남긴다. 장소 저장(save)과 동일하게 data 없이 메시지만 반환한다.
     */
    @PatchMapping("/api/v1/my/saved-places/{savedPlaceId}")
    fun visit(
        @PathVariable savedPlaceId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("방문이 완료되었습니다.")

    /**
     * 저장 장소 조회. 쿼리 파라미터
     * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준.
     * - category: 저장 카테고리 칩 필터. [SavedPlaceCategory] 이름(예: CAFE) — 잘못된 값은 400.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 형식이 잘못되면 400.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/api/v1/my/saved-places")
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: SavedPlaceCategory?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SavedPlaceListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(SavedPlaceListResponse.mock())

        return ApiResponse.success(
            SavedPlaceListResponse.from(
                savedPlaceUseCase.getSavedPlaces(
                    SavedPlacesCommand(
                        userId = userId,
                        visited = visited,
                        // 포트 계약은 카테고리 이름 문자열이다(BFF 도 쓰는 계약) — 바인딩은 enum 으로 받아 잘못된 값을 400 으로 먼저 걸러낸다.
                        category = category?.name,
                        cursor = cursor,
                        size = size,
                    ),
                ),
            ),
        )
    }
}
