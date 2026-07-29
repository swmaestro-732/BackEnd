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

    /** 목 데이터 시드 — 응답 매핑([PlaceItem]) 전 내부 표현. */
    data class MockPlace(
        val id: Long,
        val name: String,
        val imageUrl: String?,
        val categories: List<String>,
        val averageRating: Double,
        val reviewCount: Int,
        val walkingMinutes: Int,
        val latitude: Double,
        val longitude: Double,
        val hasSaved: Boolean,
    )

    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /** 성수동 일대 카페 — 디자인(검색 결과 · 장소 탭)의 예시 목록을 그대로 반영. */
        val MOCK: List<MockPlace> =
            listOf(
                MockPlace(
                    id = 101,
                    name = "어니언 성수",
                    imageUrl = image("photo-1517433670267-08bbd4be890f"),
                    categories = listOf("카페", "베이커리"),
                    averageRating = 4.8,
                    reviewCount = 1240,
                    walkingMinutes = 6,
                    latitude = 37.5445,
                    longitude = 127.0578,
                    hasSaved = true,
                ),
                MockPlace(
                    id = 102,
                    name = "콤포트 성수",
                    imageUrl = image("photo-1495474472287-4d71bcdd2085"),
                    categories = listOf("카페", "브런치"),
                    averageRating = 4.6,
                    reviewCount = 430,
                    walkingMinutes = 5,
                    latitude = 37.5432,
                    longitude = 127.0561,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 103,
                    name = "아우어베이커리 성수",
                    imageUrl = image("photo-1521017432531-fbd92d768814"),
                    categories = listOf("카페", "디저트"),
                    averageRating = 4.5,
                    reviewCount = 205,
                    walkingMinutes = 8,
                    latitude = 37.5451,
                    longitude = 127.0549,
                    hasSaved = true,
                ),
                MockPlace(
                    id = 104,
                    name = "대림창고 카페",
                    imageUrl = image("photo-1509042239860-f550ce710b93"),
                    categories = listOf("카페", "전시"),
                    averageRating = 4.6,
                    reviewCount = 320,
                    walkingMinutes = 9,
                    latitude = 37.5418,
                    longitude = 127.0592,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 105,
                    name = "센터커피 성수",
                    imageUrl = image("photo-1442512595331-e89e73853f31"),
                    categories = listOf("카페", "로스터리"),
                    averageRating = 4.7,
                    reviewCount = 512,
                    walkingMinutes = 12,
                    latitude = 37.5463,
                    longitude = 127.0537,
                    hasSaved = false,
                ),
                MockPlace(
                    id = 106,
                    name = "대성정미소 카페",
                    imageUrl = image("photo-1445116572660-236099ec97a0"),
                    categories = listOf("카페", "전시"),
                    averageRating = 4.4,
                    reviewCount = 88,
                    walkingMinutes = 14,
                    latitude = 37.5409,
                    longitude = 127.0605,
                    hasSaved = false,
                ),
            )
    }
}
