package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import java.time.Instant

/**
 * 웹 응답 DTO — 저장함 · 장소 탭 화면 조합(BFF). 프론트 화면 계약 형태.
 * 도메인 API(`GET /api/v1/my/saved-places`)의 저장 레코드·카운트·페이지 메타를 유지하고,
 * 각 저장 항목에 장소 요약(평점·카테고리·영업·거리 — 디자인 J 밴드)을 덧붙여 내려준다.
 * 현재는 컨트롤러에서 목 데이터로 채운다(실제 구현 시 user + place inbound 포트 조합으로 교체).
 */
data class SavedPlaceScreenResponse(
    // 카테고리 전체 합산 저장 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 미방문/방문 저장 개수 — 탭 배지
    val unvisitedCount: Int,
    val visitedCount: Int,
    // 카테고리별 저장 개수(0개 카테고리 제외), 개수 내림차순 — 카테고리 칩
    val categoryCounts: List<SavedPlaceCategoryCountResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedPlaces: List<SavedPlaceScreenItemResponse>,
)

/** 카테고리 칩 배지 — category 는 user 도메인 SavedPlaceCategory 의 이름 문자열(예: CAFE). */
data class SavedPlaceCategoryCountResponse(
    val category: String,
    val count: Int,
)

data class SavedPlaceScreenItemResponse(
    // 저장 레코드 id (장소 id 아님)
    val id: Long,
    val placeId: Long,
    // 저장 카테고리 스냅샷 — SavedPlaceCategory 이름 문자열
    val category: String?,
    // 방문 여부 — 미방문/방문 탭 구분
    val visited: Boolean,
    val savedAt: Instant,
    val place: SavedPlaceSummaryResponse,
)

/** 장소 요약 — 저장함 리스트 카드에 표시되는 place 도메인 정보. */
data class SavedPlaceSummaryResponse(
    val name: String,
    // 장소 카테고리 — place 도메인 PlaceCategory 이름 문자열(예: CAFE)
    val category: String,
    val area: String,
    val imageUrl: String?,
    // 거리 — 표시용 도보 소요 텍스트(예: "도보 11분"). 실구현에서 ST_Distance 미터를 변환해 생성
    val walkingTime: String,
    // 좌표 — 지도 핀 표시용(장소 상세 화면 조합과 동일 표현)
    val location: PlaceLocationResponse,
)
