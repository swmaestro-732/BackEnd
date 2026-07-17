package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceDetailResult
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
        val openStatus: String,
        val openingHoursText: String?,
        val reviewSummary: ReviewSummary,
        val viewer: Viewer,
    )

    data class Location(
        val latitude: Double,
        val longitude: Double,
    )

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

    companion object {
        fun from(result: PlaceDetailResult): PlaceDetailResponse =
            PlaceDetailResponse(
                PlaceDetail(
                    id = result.id,
                    name = result.name,
                    categories = result.categories,
                    imageUrls = result.imageUrls,
                    address = result.address,
                    location = Location(latitude = result.latitude, longitude = result.longitude),
                    openStatus = result.openStatus.name,
                    openingHoursText = result.openingHoursText,
                    reviewSummary =
                        ReviewSummary(
                            averageRating = result.reviewSummary.averageRating,
                            totalCount = result.reviewSummary.totalCount,
                            reviews =
                                result.reviewSummary.reviews.map {
                                    Review(
                                        id = it.id,
                                        author =
                                            Author(
                                                id = it.authorId,
                                                nickname = it.authorNickname,
                                                profileImageUrl = it.authorProfileImageUrl,
                                            ),
                                        rating = it.rating,
                                        content = it.content,
                                        createdAt = it.createdAt,
                                        relativeTime = it.relativeTime,
                                        photoUrls = it.photoUrls,
                                    )
                                },
                        ),
                    viewer = Viewer(hasSaved = result.viewerHasSaved),
                ),
            )
    }
}
