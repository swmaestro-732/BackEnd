package com.example.backend.bff.adapter.inbound.web.response

import java.time.Instant

/**
 * 웹 응답 DTO — 장소 상세 화면 조합(BFF). 프론트 화면 계약 형태.
 * 장소 정보 + 리뷰 요약/미리보기(작성자) + 저장 여부 + "이 근처 코스"(이 장소를 포함한 코스)를 한 번에 내려준다.
 * 현재는 컨트롤러에서 목 데이터로 채운다(실제 구현 시 place + user + course inbound 포트 조합으로 교체).
 */
data class PlaceDetailScreenResponse(
    val place: PlaceScreenResponse,
    val nearbyCourses: List<NearbyCourseResponse>,
)

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
