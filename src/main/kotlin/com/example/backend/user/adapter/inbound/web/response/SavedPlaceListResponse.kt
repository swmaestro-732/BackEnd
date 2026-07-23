package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.domain.model.SavedPlaceCategory
import java.time.Instant

/**
 * 저장 장소 조회 응답 — 노션 API 명세(User · user-place · 저장 장소 조회) 기준.
 * 노션 필드 명세 미작성 상태라 저장 레코드 스키마(saved_places)와 디자인(저장함 · 장소 탭)에서 도출.
 *
 * api-spec.md 설계 노트에 따라 이 도메인 API는 화면용이 아니라 **저장 레코드(ID 위주)**를 반환한다
 * — 장소 상세(이름·평점·거리 등)는 화면 조합 API(`GET /service/v1/my/saved-places`, 제안 상태)가 담당.
 * 페이지 메타: 커서 페이지네이션([nextCursor]/[hasNext]) + 전체 개수([totalCount], 저장함 "전체 N" 칩).
 * 카운트: 미방문/방문 탭 배지([unvisitedCount]/[visitedCount]) + 카테고리 칩 배지([categoryCounts], 0개는 제외).
 */
data class SavedPlaceListResponse(
    // 카테고리 전체 합산 저장 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 미방문 저장 개수 — 미방문 탭
    val unvisitedCount: Int,
    // 방문 저장 개수 — 방문 탭
    val visitedCount: Int,
    // 카테고리별 저장 개수(0개 카테고리 제외), 개수 내림차순 — 카테고리 칩
    val categoryCounts: List<CategoryCount>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedPlaces: List<SavedPlaceItem>,
) {
    data class CategoryCount(
        val category: SavedPlaceCategory,
        val count: Int,
    )

    data class SavedPlaceItem(
        // 저장 레코드 id (장소 id 아님)
        val id: Long,
        val placeId: Long,
        // 저장 시 place 도메인에서 복사한 장소 카테고리 스냅샷(저장함 카테고리 칩) — enum 이름 문자열로 직렬화
        val category: SavedPlaceCategory?,
        // 방문 여부 — 저장함 미방문/방문 탭 구분. PATCH 방문 처리 API로 전환된다.
        val visited: Boolean,
        val savedAt: Instant,
    )
}
