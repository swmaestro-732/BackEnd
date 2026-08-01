package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.response.SavedPlaceListResponse
import com.example.backend.user.domain.model.SavedPlaceCategory
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 저장 장소(노션 명세 · User · user-place).
 *
 * - [save] 장소 저장(`POST /service/v1/saved-places/{placeId}`): **모킹 API**.
 * - [list] 저장 장소 조회(`GET /service/v1/saved-places`): **모킹 API**. 노션 필드 명세 미작성 상태라
 *   저장 레코드 스키마(saved_places)·디자인(저장함 · 장소 탭)에서 필드를 도출했다.
 *   api-spec.md 설계 노트대로 저장 레코드(ID 위주)를 반환한다 — 화면 조합은 service 담당.
 * - [visit] 저장 장소 방문 처리(`PATCH /service/v1/saved-places/{savedPlaceId}`): **모킹 API**.
 *   노션 필드 명세 미작성 상태라 디자인(저장함 · 장소 · 방문 표시 스와이프 흐름)에서 도출 —
 *   요청 본문 없이 방문 처리하고 data 없이 메시지만 반환한다(단방향, 되돌리기 흐름 없음).
 *   경로 변수는 장소 id가 아니라 **저장 레코드 id**([SavedPlaceListResponse.SavedPlaceItem.id])다.
 *
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 경로: `/service/v1/saved-places`.
 */
@RestController
@RequestMapping("/service/v1/saved-places")
@AccessTokenRequired
class SavedPlaceController {
    @PostMapping("/{placeId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @PathVariable placeId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("장소가 저장되었습니다.")

    /**
     * 저장 장소 방문 처리(모킹). 미방문 탭 카드 스와이프 → "방문한 곳으로 표시할까요?" 확인 후 호출 —
     * 방문 탭으로 옮기고 지도에 방문 표시를 남긴다. 장소 저장(save)과 동일하게 data 없이 메시지만 반환한다.
     */
    @PatchMapping("/{savedPlaceId}")
    fun visit(
        @PathVariable savedPlaceId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("방문이 완료되었습니다.")

    /**
     * 저장 장소 조회(모킹). 항상 [SavedPlaceListResponse.mock] 전체를 최신 저장순 **고정 응답**으로 내려준다 —
     * 쿼리 파라미터는 API 계약 확인용으로 받기만 하고 동작(필터·페이지네이션)은 실구현에서 지원한다.
     *
     * 쿼리 파라미터
     * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준.
     * - category: 저장 카테고리 칩 필터. [SavedPlaceCategory] 이름(예: CAFE) — 잘못된 값은 400.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: SavedPlaceCategory?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<SavedPlaceListResponse> = ApiResponse.success(SavedPlaceListResponse.mock())
}
