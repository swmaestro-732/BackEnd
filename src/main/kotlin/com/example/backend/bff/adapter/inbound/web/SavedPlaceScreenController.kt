package com.example.backend.bff.adapter.inbound.web

import com.example.backend.bff.adapter.inbound.web.response.SavedPlaceScreenResponse
import com.example.backend.common.response.ApiResponse
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 저장함 · 장소 탭 **화면 조합 목업 API** (BFF) — `GET /service/v1/my/saved-places`.
 * api-spec.md service 후보(우선순위 높음 · 디자인 J 밴드): 저장 레코드(user) + 장소 요약(place)
 * — 이름·카테고리·지역·이미지·좌표·방문/미방문을 한 번에 내려준다. 노션 필드 명세 미작성 상태라
 * 도메인 API(`GET /api/v1/my/saved-places`) 응답 + 디자인(저장함 · 장소 탭)에서 필드를 도출했다.
 *
 * 항상 고정 목 응답을 내려준다 — 쿼리 파라미터(필터·페이지네이션)는 API 계약 확인용으로 받기만 하고
 * 동작은 실구현에서 지원한다(저장 장소 도메인 모킹과 동일 컨벤션).
 * 실제 구현 시 user + place inbound 포트 조합으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/service/v1")
class SavedPlaceScreenController {
    /**
     * 쿼리 파라미터 — 필터·페이지네이션은 도메인 저장 장소 조회와 동일 계약.
     * - visited: 방문 여부 필터(미방문/방문 탭). 생략 시 미방문(false) 기준.
     * - category: 저장 카테고리 칩 필터. SavedPlaceCategory 이름(예: CAFE).
     * - userLat/userLng: 사용자 현재 위치(선택) — 거리(walkingTime)·거리 정렬 기준. 범위 밖은 400.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/my/saved-places")
    fun getScreen(
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") userLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") userLng: Double?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<SavedPlaceScreenResponse> = ApiResponse.success(SavedPlaceScreenResponse.mock())
}
