package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import java.time.Instant

/**
 * 웹 응답 DTO — 저장함 · 장소 탭 화면 조합(BFF). 프론트 화면 계약 형태.
 * 도메인 API(`GET /service/v1/saved-places`)의 저장 레코드·카운트·페이지 메타를 유지하고,
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
) {
    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /**
         * 목 데이터 — 저장 레코드는 도메인 모킹([com.example.backend.user.adapter.inbound.web.SavedPlaceController])과,
         * 장소 정보는 장소 검색 모킹([com.example.backend.place.adapter.inbound.web.PlaceController])의
         * 목 장소(101 어니언 성수, 104 대림창고 카페, 105 센터커피 성수)와 값을 맞춰 두었다.
         * 107 자그마치·108 평화양조장은 장소 검색 목에 없어 디자인 예시 기준으로 채웠다.
         */
        private val MOCK_ITEMS: List<SavedPlaceScreenItemResponse> =
            listOf(
                SavedPlaceScreenItemResponse(
                    id = 5,
                    placeId = 108,
                    category = "BAR",
                    visited = false,
                    savedAt = Instant.parse("2026-07-17T09:20:00Z"),
                    place =
                        SavedPlaceSummaryResponse(
                            name = "평화양조장",
                            category = "BAR",
                            area = "성수동",
                            imageUrl = image("photo-1518176258769-f227c798150e"),
                            walkingTime = "도보 11분",
                            location = PlaceLocationResponse(latitude = 37.5437, longitude = 127.061),
                        ),
                ),
                SavedPlaceScreenItemResponse(
                    id = 4,
                    placeId = 107,
                    category = "CULTURE",
                    visited = false,
                    savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                    place =
                        SavedPlaceSummaryResponse(
                            name = "자그마치",
                            category = "CULTURE",
                            area = "성수동",
                            imageUrl = image("photo-1513151233558-d860c5398176"),
                            walkingTime = "도보 10분",
                            location = PlaceLocationResponse(latitude = 37.5426, longitude = 127.0554),
                        ),
                ),
                SavedPlaceScreenItemResponse(
                    id = 3,
                    placeId = 105,
                    category = "CAFE",
                    visited = false,
                    savedAt = Instant.parse("2026-07-12T05:30:00Z"),
                    place =
                        SavedPlaceSummaryResponse(
                            name = "센터커피 성수",
                            category = "CAFE",
                            area = "성수동",
                            imageUrl = image("photo-1442512595331-e89e73853f31"),
                            walkingTime = "도보 12분",
                            location = PlaceLocationResponse(latitude = 37.5463, longitude = 127.0537),
                        ),
                ),
                SavedPlaceScreenItemResponse(
                    id = 2,
                    placeId = 104,
                    category = "CAFE",
                    visited = true,
                    savedAt = Instant.parse("2026-07-08T11:00:00Z"),
                    place =
                        SavedPlaceSummaryResponse(
                            name = "대림창고 카페",
                            category = "CAFE",
                            area = "성수동",
                            imageUrl = image("photo-1509042239860-f550ce710b93"),
                            walkingTime = "도보 9분",
                            location = PlaceLocationResponse(latitude = 37.5418, longitude = 127.0592),
                        ),
                ),
                SavedPlaceScreenItemResponse(
                    id = 1,
                    placeId = 101,
                    category = "CAFE",
                    visited = true,
                    savedAt = Instant.parse("2026-07-05T02:15:00Z"),
                    place =
                        SavedPlaceSummaryResponse(
                            name = "어니언 성수",
                            category = "CAFE",
                            area = "성수동",
                            imageUrl = image("photo-1517433670267-08bbd4be890f"),
                            walkingTime = "도보 6분",
                            location = PlaceLocationResponse(latitude = 37.5445, longitude = 127.0578),
                        ),
                ),
            )

        /** 저장함 · 장소 탭 화면 조합 목 — 항상 고정 목 응답(nextCursor=null·hasNext=false). */
        fun mock(): SavedPlaceScreenResponse =
            SavedPlaceScreenResponse(
                totalCount = MOCK_ITEMS.size,
                unvisitedCount = MOCK_ITEMS.count { !it.visited },
                visitedCount = MOCK_ITEMS.count { it.visited },
                categoryCounts =
                    MOCK_ITEMS
                        .mapNotNull { it.category }
                        .groupingBy { it }
                        .eachCount()
                        .map { (category, count) -> SavedPlaceCategoryCountResponse(category, count) }
                        .sortedByDescending { it.count },
                nextCursor = null,
                hasNext = false,
                savedPlaces = MOCK_ITEMS,
            )
    }
}

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
