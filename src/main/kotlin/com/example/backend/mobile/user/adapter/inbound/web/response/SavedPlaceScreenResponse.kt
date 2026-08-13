package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.mobile.user.application.port.inbound.dto.SavedPlaceScreenResult
import java.time.Instant

/**
 * 웹 응답 DTO — 저장함 · 장소 탭 화면 조합(BFF). 프론트 화면 계약 형태.
 * 도메인 조회(`GET /api/v1/my/saved-places`)의 저장 레코드·카운트·페이지 메타를 유지하고,
 * 각 저장 항목에 장소 요약(이름·카테고리·지역·이미지·좌표 — 디자인 J 밴드)을 덧붙여 내려준다.
 * [from] 이 포트 결과([SavedPlaceScreenResult])를 매핑하고, `?mock=true` 는 [mock] 고정 목을 쓴다.
 */
data class SavedPlaceScreenResponse(
    // 카테고리 전체 합산 저장 개수 — 저장함 "전체 N" 칩
    val totalCount: Long,
    // 미방문/방문 저장 개수 — 탭 배지
    val unvisitedCount: Long,
    val visitedCount: Long,
    // 카테고리별 저장 개수(0개 카테고리 제외), 개수 내림차순 — 카테고리 칩
    val categoryCounts: List<SavedPlaceCategoryCountResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedPlaces: List<SavedPlaceScreenItemResponse>,
) {
    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /** 화면 조합 결과를 응답 계약으로 매핑한다. */
        fun from(result: SavedPlaceScreenResult): SavedPlaceScreenResponse =
            SavedPlaceScreenResponse(
                totalCount = result.totalCount,
                unvisitedCount = result.unvisitedCount,
                visitedCount = result.visitedCount,
                categoryCounts =
                    result.categoryCounts.map {
                        SavedPlaceCategoryCountResponse(category = it.category, count = it.count)
                    },
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                savedPlaces =
                    result.items.map { item ->
                        SavedPlaceScreenItemResponse(
                            id = item.id,
                            placeId = item.placeId,
                            category = item.category,
                            visited = item.visited,
                            savedAt = item.savedAt,
                            place =
                                SavedPlaceSummaryResponse(
                                    name = item.place.name,
                                    category = item.place.category,
                                    area = item.place.area,
                                    imageUrl = item.place.imageUrl,
                                    // 거리는 1차 구현 범위 밖 — 항상 null 이다.
                                    walkingTime = null,
                                    location =
                                        PlaceLocationResponse(
                                            latitude = item.place.latitude,
                                            longitude = item.place.longitude,
                                        ),
                                ),
                        )
                    },
            )

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
                totalCount = MOCK_ITEMS.size.toLong(),
                unvisitedCount = MOCK_ITEMS.count { !it.visited }.toLong(),
                visitedCount = MOCK_ITEMS.count { it.visited }.toLong(),
                categoryCounts =
                    MOCK_ITEMS
                        .mapNotNull { it.category }
                        .groupingBy { it }
                        .eachCount()
                        .map { (category, count) -> SavedPlaceCategoryCountResponse(category, count.toLong()) }
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
    val count: Long,
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
    // 표시용 지역 이름(읍면동 등). 장소의 area_code 가 없거나 해석되지 않으면 null
    val area: String?,
    val imageUrl: String?,
    // 거리 — 표시용 도보 소요 텍스트(예: "도보 11분"). 1차 구현 범위 밖이라 실조회 응답은 항상 null(목은 값이 있다).
    val walkingTime: String?,
    // 좌표 — 지도 핀 표시용(장소 상세 화면 조합과 동일 표현)
    val location: PlaceLocationResponse,
)
