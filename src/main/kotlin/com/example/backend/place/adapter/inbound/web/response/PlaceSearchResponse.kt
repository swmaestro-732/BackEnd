package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage

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

    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /**
         * 실구현 검색 결과([PlaceSummaryPage]) → 응답 매핑. 커서 페이지네이션을 반영한다 —
         * [nextCursor] 는 서비스가 발급한 불투명 커서를 그대로 내려준다(경로별 형식은 웹 계층이 모른다),
         * [totalCount] 는 검색어에 매칭되는 전체 개수다. 뷰포트 필터·거리 정렬은 후속 과제.
         *
         * 리뷰/저장 필드(averageRating·reviewCount·hasSaved)와 walkingMinutes 는 아직 소스가 없어 기본값으로 둔다(후속).
         */
        fun from(page: PlaceSummaryPage): PlaceSearchResponse =
            PlaceSearchResponse(
                totalCount = page.totalCount,
                nextCursor = page.nextCursor,
                hasNext = page.hasNext,
                places = page.items.map { it.toItem() },
            )

        private fun PlaceSummary.toItem() =
            PlaceItem(
                id = id,
                name = name,
                imageUrl = imageUrl,
                categories = listOf(category),
                averageRating = 0.0,
                reviewCount = 0,
                walkingMinutes = null,
                location = Location(latitude, longitude),
                hasSaved = false,
            )

        /**
         * 모킹 검색 응답 — 성수동 일대 카페 고정 목록(디자인 · 검색 결과 · 장소 탭 예시 반영).
         * 시드 데이터가 없는 개발 환경용 폴백으로 [com.example.backend.place.adapter.inbound.web.PlaceController]가 호출한다.
         * 필터·정렬·페이지네이션은 적용하지 않고 전체를 한 페이지로 내려준다(nextCursor=null, hasNext=false).
         */
        fun mock(): PlaceSearchResponse {
            val places =
                listOf(
                    PlaceItem(
                        id = 101,
                        name = "어니언 성수",
                        imageUrl = image("photo-1517433670267-08bbd4be890f"),
                        categories = listOf("카페", "베이커리"),
                        averageRating = 4.8,
                        reviewCount = 1240,
                        walkingMinutes = 6,
                        location = Location(37.5445, 127.0578),
                        hasSaved = true,
                    ),
                    PlaceItem(
                        id = 102,
                        name = "콤포트 성수",
                        imageUrl = image("photo-1495474472287-4d71bcdd2085"),
                        categories = listOf("카페", "브런치"),
                        averageRating = 4.6,
                        reviewCount = 430,
                        walkingMinutes = 5,
                        location = Location(37.5432, 127.0561),
                        hasSaved = false,
                    ),
                    PlaceItem(
                        id = 103,
                        name = "아우어베이커리 성수",
                        imageUrl = image("photo-1521017432531-fbd92d768814"),
                        categories = listOf("카페", "디저트"),
                        averageRating = 4.5,
                        reviewCount = 205,
                        walkingMinutes = 8,
                        location = Location(37.5451, 127.0549),
                        hasSaved = true,
                    ),
                    PlaceItem(
                        id = 104,
                        name = "대림창고 카페",
                        imageUrl = image("photo-1509042239860-f550ce710b93"),
                        categories = listOf("카페", "전시"),
                        averageRating = 4.6,
                        reviewCount = 320,
                        walkingMinutes = 9,
                        location = Location(37.5418, 127.0592),
                        hasSaved = false,
                    ),
                    PlaceItem(
                        id = 105,
                        name = "센터커피 성수",
                        imageUrl = image("photo-1442512595331-e89e73853f31"),
                        categories = listOf("카페", "로스터리"),
                        averageRating = 4.7,
                        reviewCount = 512,
                        walkingMinutes = 12,
                        location = Location(37.5463, 127.0537),
                        hasSaved = false,
                    ),
                    PlaceItem(
                        id = 106,
                        name = "대성정미소 카페",
                        imageUrl = image("photo-1445116572660-236099ec97a0"),
                        categories = listOf("카페", "전시"),
                        averageRating = 4.4,
                        reviewCount = 88,
                        walkingMinutes = 14,
                        location = Location(37.5409, 127.0605),
                        hasSaved = false,
                    ),
                )
            return PlaceSearchResponse(
                totalCount = places.size,
                nextCursor = null,
                hasNext = false,
                places = places,
            )
        }
    }
}
