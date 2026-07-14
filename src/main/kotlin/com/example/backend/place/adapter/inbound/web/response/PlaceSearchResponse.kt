package com.example.backend.place.adapter.inbound.web.response

/**
 * 장소 검색 응답 — 노션 API 명세(Place · 장소 검색) 기준, 필드는 디자인(검색 결과 · 장소 탭/지도)에서 도출.
 *
 * 목록 카드: 썸네일·이름·평점(★)·카테고리 나열·도보 시간·저장 북마크.
 * 지도 핀: 각 항목의 [PlaceItem.location] 좌표로 찍는다.
 * 페이지 메타: 커서 페이지네이션([nextCursor]/[hasNext]) + 전체 개수([totalCount], "장소 N곳").
 */
data class PlaceSearchResponse(
    val totalCount: Int,
    val nextCursor: String?,
    val hasNext: Boolean,
    val places: List<PlaceItem>,
) {
    data class PlaceItem(
        val id: Long,
        val name: String,
        val imageUrl: String?,
        val categories: List<String>,
        val averageRating: Double,
        // 리뷰순(REVIEW) 정렬 기준값. 카드에 직접 노출되진 않지만 정렬 근거로 내려준다.
        val reviewCount: Int,
        // "도보 N분" — 사용자 위치(userLat/userLng)가 있을 때만 계산, 없으면 null.
        val walkingMinutes: Int?,
        val location: Location,
        val hasSaved: Boolean,
    )

    data class Location(
        val latitude: Double,
        val longitude: Double,
    )
}
