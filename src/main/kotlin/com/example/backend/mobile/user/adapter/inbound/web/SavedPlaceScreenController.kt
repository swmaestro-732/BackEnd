package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedPlaceCategoryCountResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedPlaceScreenItemResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedPlaceScreenResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedPlaceSummaryResponse
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 저장함 · 장소 탭 **화면 조합 목업 API** (BFF) — `GET /service/v1/my/saved-places`.
 * api-spec.md service 후보(우선순위 높음 · 디자인 J 밴드): 저장 레코드(user) + 장소 요약(place)
 * — 이름·카테고리·지역·이미지·좌표·방문/미방문을 한 번에 내려준다. 노션 필드 명세 미작성 상태라
 * 도메인 API(`GET /api/v1/my/saved-places`) 응답 + 디자인(저장함 · 장소 탭)에서 필드를 도출했다.
 *
 * 항상 고정 목 응답을 내려준다 — 쿼리 파라미터(필터·페이지네이션)는 API 계약 확인용으로 받기만 하고
 * 동작은 실구현에서 지원한다(저장 장소 도메인 모킹과 동일 컨벤션).
 * 실제 구현 시 user + place inbound 포트 조합으로 교체하고 [MockErrors] 호출을 제거한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
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
    @GetMapping("/places/save")
    fun getScreen(
        @RequestParam(required = false) visited: Boolean = false,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") userLat: Double?,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") userLng: Double?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SavedPlaceScreenResponse> {
        MockErrors.throwIfRequested(mockError)

        return ApiResponse.success(
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
            ),
        )
    }

    private companion object {
        fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /**
         * 목 데이터 — 저장 레코드는 도메인 모킹([com.example.backend.user.adapter.inbound.web.SavedPlaceController])과,
         * 장소 정보는 장소 검색 모킹([com.example.backend.place.adapter.inbound.web.PlaceController])의
         * 목 장소(101 어니언 성수, 104 대림창고 카페, 105 센터커피 성수)와 값을 맞춰 두었다.
         * 107 자그마치·108 평화양조장은 장소 검색 목에 없어 디자인 예시 기준으로 채웠다.
         */
        val MOCK_ITEMS: List<SavedPlaceScreenItemResponse> =
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
    }
}
