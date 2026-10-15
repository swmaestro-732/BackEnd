package com.example.backend.mobile.user.application.port.inbound.dto

import java.time.Instant

/**
 * 저장함 장소 탭 화면 조합 결과 (BFF) — 저장 레코드 + 배지 카운트 + 페이지 메타 + 항목별 장소 요약.
 *
 * 카운트([totalCount]/[unvisitedCount]/[visitedCount]/[categoryCounts])는 조회 필터(visited·category)와
 * 무관한 전체 기준이다(탭·칩 배지는 어느 탭에서도 같은 값 — 도메인 조회 계약을 그대로 잇는다).
 */
data class SavedPlaceScreenResult(
    val totalCount: Long,
    val unvisitedCount: Long,
    val visitedCount: Long,
    val categoryCounts: List<CategoryCount>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val items: List<Item>,
) {
    /** category 는 저장 카테고리 이름 문자열(예: CAFE) — BFF 는 user 도메인 enum 을 참조할 수 없다. */
    data class CategoryCount(
        val category: String,
        val count: Long,
    )

    /**
     * 저장 항목 하나 — 저장 레코드(user)에 장소 요약([place])을 붙인 것.
     * 장소를 해석하지 못한 항목(삭제된 장소 등)은 목록에서 빠지므로 [place] 는 non-null 이다.
     */
    data class Item(
        // 저장 레코드 id (장소 id 아님)
        val id: Long,
        val placeId: Long,
        // 저장 시 복사한 카테고리 스냅샷 이름(미분류면 null)
        val category: String?,
        val visited: Boolean,
        val savedAt: Instant,
        val place: Place,
    )

    /**
     * 장소 요약 — 저장함 리스트 카드 표시용. place 도메인 요약 + area 도메인에서 해석한 지역 이름.
     * 거리(도보 소요)는 1차 구현 범위 밖이라 담지 않는다.
     */
    data class Place(
        val name: String,
        // place 도메인 PlaceCategory 이름 문자열
        val category: String,
        // 읍면동 등 표시용 지역 이름. 장소의 area_code 가 없거나 해석되지 않으면 null
        val area: String?,
        val imageUrl: String?,
        val latitude: Double,
        val longitude: Double,
    )
}
