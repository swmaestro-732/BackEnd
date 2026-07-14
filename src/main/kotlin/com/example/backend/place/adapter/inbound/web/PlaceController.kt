package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.place.adapter.inbound.web.response.PlaceDetailResponse
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 인바운드 어댑터 — 장소 상세(노션 명세 · Place · 장소 상세).
 * Result → Response 매핑만 담당하고 조합·정책은 [PlaceQueryUseCase] 구현이 가진다.
 *
 * `?mock=true` 는 DB에 데이터가 없는 동안 프론트 개발용 **모킹 폴백** — 고정 페이로드를 반환한다.
 * 시드 데이터가 확보되면 파라미터와 [mockPlace] 를 제거한다.
 */
@RestController
@RequestMapping("/api/v1/places")
class PlaceController(
    private val placeQueryUseCase: PlaceQueryUseCase,
) {
    @GetMapping("/{placeId}")
    fun detail(
        @PathVariable placeId: Long,
        @RequestParam(required = false, defaultValue = "false") mock: Boolean,
    ): ApiResponse<PlaceDetailResponse> =
        if (mock) {
            ApiResponse.success(PlaceDetailResponse(mockPlace(placeId)))
        } else {
            ApiResponse.success(PlaceDetailResponse.from(placeQueryUseCase.getDetail(placeId)))
        }

    // 모킹 폴백 페이로드 — 노션 명세(Place · 장소 상세) 기준 목업(#15와 동일 데이터).
    private fun mockPlace(placeId: Long) =
        PlaceDetailResponse.PlaceDetail(
            id = placeId,
            name = "어니언 성수",
            categories = listOf("카페", "베이커리"),
            imageUrls =
                listOf(
                    "https://cdn.example.com/places/$placeId/1.jpg",
                    "https://cdn.example.com/places/$placeId/2.jpg",
                ),
            address = "서울 성동구 아차산로 100",
            location = PlaceDetailResponse.Location(latitude = 37.5446, longitude = 127.0559),
            openStatus = "OPEN",
            openingHoursText = "매일 11:00 – 21:00",
            reviewSummary =
                PlaceDetailResponse.ReviewSummary(
                    averageRating = 4.8,
                    totalCount = 128,
                    reviews =
                        listOf(
                            PlaceDetailResponse.Review(
                                id = 1,
                                author =
                                    PlaceDetailResponse.Author(
                                        id = 10,
                                        nickname = "현우님",
                                        profileImageUrl = "https://cdn.example.com/users/10.jpg",
                                    ),
                                rating = 5,
                                content = "팡도르가 정말 맛있어요. 통창 자리 뷰도 최고. 웨이팅은 조금 있었어요.",
                                createdAt = Instant.parse("2026-07-08T04:20:00Z"),
                                relativeTime = "3일 전",
                                photoUrls = emptyList(),
                            ),
                            PlaceDetailResponse.Review(
                                id = 2,
                                author =
                                    PlaceDetailResponse.Author(
                                        id = 11,
                                        nickname = "커피러버",
                                        profileImageUrl = null,
                                    ),
                                rating = 4,
                                content = "빵이 다양하고 공간이 넓어요.",
                                createdAt = Instant.parse("2026-07-06T09:00:00Z"),
                                relativeTime = "5일 전",
                                photoUrls = emptyList(),
                            ),
                        ),
                ),
            viewer = PlaceDetailResponse.Viewer(hasSaved = false),
        )
}
