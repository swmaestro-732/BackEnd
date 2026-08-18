package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
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
 * 네 엔드포인트가 저장 장소(saved-places)라는 한 리소스를 공유한다(노션 명세 기준) — 액션 경로
 * (`/api/v1/places/save`)·개인 네임스페이스(`/api/v1/my/saved-places`)로 갈려 있던 것을
 * `/api/v1/saved-places` 하나로 합쳤다. 클래스 레벨 매핑 대신 메서드 레벨 전체 경로로 둔다.
 *
 * - [save] 장소 저장(`POST /api/v1/saved-places`): **실구현** — 인바운드 포트([SavedPlaceUseCase])로 저장한다.
 *   장소 존재를 검증하고(그 외 404), 이미 저장한 장소면 중복 저장으로 막는다(409). 장소당 저장 레코드는 1개다.
 *   저장 주체 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다.
 * - [unsave] 장소 저장 취소(`DELETE /api/v1/saved-places/{placeId}`): **실구현** — 저장의 역연산이라
 *   placeId 로 (user, place) 저장 레코드를 지운다. 저장돼 있지 않아도 오류 없이 성공한다(멱등 — 코스 저장 취소 선례와 동일).
 * - [list] 저장 장소 조회(`GET /api/v1/saved-places`): **실구현** — 인바운드 포트([SavedPlaceUseCase])로 조회한다.
 *   저장 레코드(ID 위주)를 최신 저장순으로 커서 페이지네이션해 반환한다 —
 *   장소 상세(이름·평점·거리 등)는 화면 조합 API(`GET /service/v1/my/saved-places`)가 담당한다.
 * - [visit] 저장 장소 방문 처리(`PATCH /api/v1/saved-places/{placeId}`): **모킹 API**(실구현은 별도 티켓).
 *   경로 변수는 [unsave] 와 같은 **장소 id** 다 — 같은 경로 모양에서 메서드만 다른 두 액션이
 *   서로 다른 id 를 받으면 헷갈리므로, 저장 레코드 id 가 아니라 placeId 로 통일했다.
 *
 * 경로가 `/api/v1/my` 밖으로 나왔으므로 SecurityConfig 가 `/api/v1/saved-places` 를 인증 필수 경로로
 * 따로 등록한다 — `/api` 하위 permitAll 에 걸려 조회·방문 처리가 무인증으로 열리는 것을 막는다.
 * 쓰기(저장·취소)는 그에 더해 [AccessTokenRequired] 메서드 시큐리티로 access 토큰을 강제한다 —
 * 회원가입 목적 토큰(purpose != access)으로는 남의 저장함에 쓸 수 없다(UserController 선례와 동일).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 저장/조회 없이 고정 목([SavedPlaceListResponse.mock])을
 * 반환한다(코스 저장 선례와 동일 규칙). 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
class SavedPlaceController(
    private val savedPlaceUseCase: SavedPlaceUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/saved-places")
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun save(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: SavePlaceRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("장소가 저장되었습니다.")

        savedPlaceUseCase.save(userId, request.placeId)
        return ApiResponse.ok("장소가 저장되었습니다.")
    }

    @DeleteMapping("/api/v1/saved-places/{placeId}")
    @AccessTokenRequired
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
     *
     * 경로 변수는 [unsave] 와 같은 장소 id — 저장 레코드는 (user, place) 로 유일하므로 placeId 만으로 특정된다.
     */
    @PatchMapping("/api/v1/saved-places/{placeId}")
    fun visit(
        @PathVariable placeId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("방문이 완료되었습니다.")

    /**
     * 저장 장소 조회. 쿼리 파라미터
     * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준.
     * - category: 저장 카테고리 칩 필터. [SavedPlaceCategory] 이름(예: CAFE) — 잘못된 값은 400.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 형식이 잘못되면 400.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/api/v1/saved-places")
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
