package com.example.backend.place.adapter.inbound.web.response

import java.time.Instant

/**
 * 장소 상세 응답 — 노션 API 명세서(Place · 장소 상세) 합의 스펙.
 * 필드는 디자인(비로그인 장소 상세 화면)에서 도출: 이미지 캐러셀·카테고리·평점/후기 수·
 * 길찾기(좌표)·주소·영업 상태·후기 미리보기 2개·저장 여부.
 */
data class PlaceDetailResponse(
    val place: PlaceDetail,
) {
    data class PlaceDetail(
        val id: Long,
        val name: String,
        val categories: List<String>,
        val imageUrls: List<String>,
        val address: String,
        val location: Location,
        val openStatus: OpenStatus,
        val openingHoursText: String?,
        val reviewSummary: ReviewSummary,
        val viewer: Viewer,
    )

    data class Location(
        val latitude: Double,
        val longitude: Double,
    )

    /** 영업 상태 — 응답은 String 직렬화, 요청에 쓰이면 Enum으로 받는다(api-design 정책). */
    enum class OpenStatus { OPEN, CLOSED }

    data class ReviewSummary(
        val averageRating: Double,
        val totalCount: Int,
        val reviews: List<Review>,
    )

    data class Review(
        val id: Long,
        val author: Author,
        val rating: Int,
        val content: String,
        val createdAt: Instant,
        val relativeTime: String,
        val photoUrls: List<String>,
    )

    data class Author(
        val id: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )

    data class Viewer(
        val hasSaved: Boolean,
    )
}
