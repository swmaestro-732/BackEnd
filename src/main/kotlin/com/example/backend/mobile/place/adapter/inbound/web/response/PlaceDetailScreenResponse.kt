package com.example.backend.mobile.place.adapter.inbound.web.response

import com.example.backend.mobile.place.application.port.inbound.dto.PlaceDetailScreenResult
import java.time.Instant

/**
 * 웹 응답 DTO — 장소 상세 화면 조합(BFF). 프론트 화면 계약 형태.
 * 장소 정보 + 리뷰 요약/미리보기(작성자) + 저장 여부 + "이 근처 코스"(이 장소를 포함한 코스)를 한 번에 내려준다.
 * 실구현은 place 도메인 인바운드 포트로 장소를 조회해 채운다([from]). 리뷰·이 근처 코스·저장 여부는 아직
 * 백엔드가 없어 빈/false 스텁으로 채운다(MVP 범위). `?mock=true` 폴백은 고정 목([MOCK])을 반환한다.
 */
data class PlaceDetailScreenResponse(
    val place: PlaceScreenResponse,
    val nearbyCourses: List<NearbyCourseResponse>,
) {
    companion object {
        /** 실구현 매핑 — 장소 재료만 채우고 리뷰·이 근처 코스·저장 여부는 빈/false 스텁. */
        fun from(result: PlaceDetailScreenResult) =
            PlaceDetailScreenResponse(
                place =
                    PlaceScreenResponse(
                        id = result.id,
                        name = result.name,
                        categories = listOf(result.category),
                        imageUrls = result.imageUrl?.let { listOf(it) } ?: emptyList(),
                        address = result.address,
                        location = PlaceLocationResponse(latitude = result.latitude, longitude = result.longitude),
                        openStatus = "UNKNOWN",
                        openingHoursText = null,
                        reviewSummary =
                            PlaceReviewSummaryResponse(
                                averageRating = 0.0,
                                totalCount = 0,
                                reviews = emptyList(),
                            ),
                        viewer = PlaceViewerResponse(hasSaved = false),
                    ),
                nearbyCourses = emptyList(),
            )

        /** 시드/DB 없이 프론트가 붙어볼 수 있는 고정 목(`?mock=true`). */
        val MOCK =
            PlaceDetailScreenResponse(
                place =
                    PlaceScreenResponse(
                        id = 101,
                        name = "어니언 성수",
                        categories = listOf("카페", "베이커리"),
                        imageUrls =
                            listOf(
                                "https://cdn.example.com/places/101/1.jpg",
                                "https://cdn.example.com/places/101/2.jpg",
                            ),
                        address = "서울 성동구 아차산로 100",
                        location = PlaceLocationResponse(latitude = 37.5446, longitude = 127.0559),
                        openStatus = "OPEN",
                        openingHoursText = "매일 11:00 – 21:00",
                        reviewSummary =
                            PlaceReviewSummaryResponse(
                                averageRating = 4.8,
                                totalCount = 128,
                                reviews =
                                    listOf(
                                        PlaceReviewResponse(
                                            id = 1,
                                            author =
                                                PlaceReviewAuthorResponse(
                                                    id = 10,
                                                    nickname = "현우님",
                                                    profileImageUrl = "https://cdn.example.com/users/10.jpg",
                                                ),
                                            rating = 5,
                                            content = "팡도르가 정말 맛있어요. 통창 자리 뷰도 최고. 웨이팅은 조금 있었어요.",
                                            createdAt = Instant.parse("2026-07-08T04:20:00Z"),
                                            relativeTime = "9일 전",
                                            photoUrls = emptyList(),
                                        ),
                                        PlaceReviewResponse(
                                            id = 2,
                                            author =
                                                PlaceReviewAuthorResponse(
                                                    id = 11,
                                                    nickname = "커피러버",
                                                    profileImageUrl = null,
                                                ),
                                            rating = 4,
                                            content = "빵이 다양하고 공간이 넓어요.",
                                            createdAt = Instant.parse("2026-07-06T09:00:00Z"),
                                            relativeTime = "11일 전",
                                            photoUrls = emptyList(),
                                        ),
                                    ),
                            ),
                        viewer = PlaceViewerResponse(hasSaved = false),
                    ),
                nearbyCourses =
                    listOf(
                        NearbyCourseResponse(
                            id = 1,
                            title = "비 오는 날 성수 감성 카페 코스",
                            coverImageUrl = "https://cdn.example.com/courses/1/cover.jpg",
                            placeCount = 4,
                            authorNickname = "지호님",
                        ),
                        NearbyCourseResponse(
                            id = 2,
                            title = "성수 베이커리 투어",
                            coverImageUrl = "https://cdn.example.com/courses/2/cover.jpg",
                            placeCount = 5,
                            authorNickname = "빵순이",
                        ),
                    ),
            )
    }
}

data class PlaceScreenResponse(
    val id: Long,
    val name: String,
    val categories: List<String>,
    val imageUrls: List<String>,
    val address: String,
    val location: PlaceLocationResponse,
    val openStatus: String,
    val openingHoursText: String?,
    val reviewSummary: PlaceReviewSummaryResponse,
    val viewer: PlaceViewerResponse,
)

data class PlaceLocationResponse(
    val latitude: Double,
    val longitude: Double,
)

/** 리뷰 섹션 — 장소 전체 집계 + 최신순 미리보기. 전체 목록은 `/api/v1/places/{id}/reviews` 로 조회한다. */
data class PlaceReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Int,
    val reviews: List<PlaceReviewResponse>,
)

data class PlaceReviewResponse(
    val id: Long,
    val author: PlaceReviewAuthorResponse,
    val rating: Int,
    val content: String,
    val createdAt: Instant,
    val relativeTime: String,
    val photoUrls: List<String>,
)

/** 리뷰 작성자 — 간략 정보(닉네임·프로필 이미지). 프로필 이미지는 없을 수 있다. */
data class PlaceReviewAuthorResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String?,
)

/** 로그인 사용자 관점 상태(저장 여부). JWT 도입 전에는 false 스텁. */
data class PlaceViewerResponse(
    val hasSaved: Boolean,
)

/** 이 장소를 포함한 코스 — "이 근처 코스" 섹션. */
data class NearbyCourseResponse(
    val id: Long,
    val title: String,
    val coverImageUrl: String,
    val placeCount: Int,
    val authorNickname: String,
)
