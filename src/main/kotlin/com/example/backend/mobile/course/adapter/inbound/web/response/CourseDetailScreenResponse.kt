package com.example.backend.mobile.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 웹 응답 DTO — 코스 상세 화면 조합(BFF). 프론트 화면 계약 형태.
 * 코스 상세([CourseDetailResult]) + 작성자 프로필([UserProfileResult]) + 장소 요약([PlaceSummary])을
 * 조합해 채운다. 표시 로직(도보 시간 합계·따라가기 축약 라벨·장소명/카테고리 결합)은 이 계층의 [from] 매퍼가 담당한다.
 * `reviewSummary` 는 아직 리뷰 조회 유스케이스가 없어 실 응답에서는 null 로 내려간다(유스케이스 도입 시 result 에서 매핑).
 * 목([MOCK]) 응답에만 고정 예시를 채워 프론트가 형태를 확인할 수 있게 한다.
 */
data class CourseDetailScreenResponse(
    val course: CourseScreenResponse,
    val reviewSummary: ReviewSummaryResponse?,
) {
    companion object {
        /**
         * 화면 조합 결과([CourseDetailScreenResult]) → 응답 매핑.
         * `reviewSummary` 는 리뷰 조회 유스케이스가 없어 아직 null 이다 — 목 값([MOCK])을 실 응답에 노출하지 않는다
         * (유스케이스 도입 시 result 에서 매핑).
         */
        fun from(result: CourseDetailScreenResult): CourseDetailScreenResponse =
            CourseDetailScreenResponse(
                course = CourseScreenResponse.from(result.course, result.author, result.places),
                reviewSummary = null,
            )

        private fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        private fun img(
            token: String,
            orderNo: Int,
        ) = CoursePlaceImageResponse(image(token), orderNo)

        private fun kst(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ) = OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(9))

        private const val PROFILE_IMAGE =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQi7ZSFKA2brmDYt72J8vLDQxgOJKxs-lj4tavhXo_pEA&s=10"

        /**
         * `?mock=true` 폴백 응답 — 시드/DB 없이 프론트가 붙어볼 수 있게 고정 화면 목을 내려준다.
         * 코스 상세 목([com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse.MOCK])과 같은 코스
         * (비 오는 날 성수 감성 카페 코스)로 값을 맞춰 두었다. `reviewSummary` 는 리뷰 조회 유스케이스 도입 전까지 이 목을 그대로 쓴다.
         */
        val MOCK: CourseDetailScreenResponse =
            CourseDetailScreenResponse(
                course =
                    CourseScreenResponse(
                        id = "1",
                        title = "비 오는 날 성수 감성 카페 코스",
                        coverImageUrl = image("HDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                        themes = listOf("데이트"),
                        tags = listOf("감성카페", "비오는날", "성수동"),
                        description =
                            "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, " +
                                "장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                        stats = CourseStatsResponse(placeCount = 4, walkingMinutes = 14, tracingCount = 1200),
                        author =
                            AuthorResponse(
                                id = 1,
                                nickname = "지호님",
                                handle = "jiho_routes",
                                profileImageUrl = PROFILE_IMAGE,
                                isFollowing = false,
                                isFollower = true,
                            ),
                        places =
                            listOf(
                                CoursePlaceScreenResponse(
                                    id = 1,
                                    placeId = 101,
                                    orderNo = 0,
                                    name = "어니언 성수",
                                    caption = "통창 자리가 명당이에요. 비 오는 날 앉으면 뷰가 최고.",
                                    walkingMinutesToNext = 6,
                                    categories = listOf("카페", "베이커리"),
                                    location = PlaceLocationResponse(latitude = 37.5445, longitude = 127.0575),
                                    images =
                                        listOf(
                                            img("HIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA", 0),
                                            img("ri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw", 1),
                                        ),
                                ),
                                CoursePlaceScreenResponse(
                                    id = 2,
                                    placeId = 102,
                                    orderNo = 1,
                                    name = "대림창고 갤러리",
                                    caption = "안쪽 전시 공간 꼭 들러보세요.",
                                    walkingMinutesToNext = 3,
                                    categories = listOf("카페", "전시"),
                                    location = PlaceLocationResponse(latitude = 37.5419, longitude = 127.0555),
                                    images = listOf(img("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw", 0)),
                                ),
                                CoursePlaceScreenResponse(
                                    id = 3,
                                    placeId = 103,
                                    orderNo = 2,
                                    name = "센터커피 성수",
                                    caption = "원두 향이 좋아요. 2층 창가 추천.",
                                    walkingMinutesToNext = 5,
                                    images = listOf(img("TMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw", 0)),
                                    categories = listOf("카페"),
                                    location = PlaceLocationResponse(latitude = 37.5447, longitude = 127.0533),
                                ),
                                CoursePlaceScreenResponse(
                                    id = 4,
                                    placeId = 104,
                                    orderNo = 3,
                                    name = "카페 할아버지공장",
                                    caption = "마무리로 딱. 넓어서 웨이팅 걱정 없어요.",
                                    walkingMinutesToNext = null,
                                    categories = listOf("카페", "베이커리"),
                                    location = PlaceLocationResponse(latitude = 37.5463, longitude = 127.0662),
                                    images =
                                        listOf(
                                            img("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA", 0),
                                            img("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw", 1),
                                        ),
                                ),
                            ),
                        viewer = CourseViewerResponse(hasSaved = false, hasStartedCourse = false),
                    ),
                reviewSummary =
                    ReviewSummaryResponse(
                        averageRating = 4.3,
                        totalCount = 6,
                        hasCompletedCourse = true,
                        ratingDistribution =
                            listOf(
                                RatingCountResponse(5, 3),
                                RatingCountResponse(4, 2),
                                RatingCountResponse(3, 1),
                                RatingCountResponse(2, 0),
                                RatingCountResponse(1, 0),
                            ),
                        previews =
                            listOf(
                                ReviewPreviewResponse(
                                    id = "1",
                                    author =
                                        ReviewAuthorResponse(
                                            id = 2,
                                            nickname = "성수러버",
                                            profileImageUrl = PROFILE_IMAGE,
                                        ),
                                    rating = 5,
                                    content = "비 오는 날 딱이에요. 통창 자리 순서대로 도니 동선도 완벽했어요. 웨이팅도 거의 없었어요 🌧️",
                                    createdAt = kst(2026, 7, 7, 13, 20),
                                    relativeTime = "4일 전",
                                    photoUrls =
                                        listOf(
                                            image("ri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw"),
                                            image("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw"),
                                        ),
                                    tags =
                                        listOf(
                                            ReviewTagResponse("구성이 알차요", "packed"),
                                            ReviewTagResponse("흐름이 자연스러워요", "smooth"),
                                        ),
                                ),
                                ReviewPreviewResponse(
                                    id = "2",
                                    author =
                                        ReviewAuthorResponse(
                                            id = 3,
                                            nickname = "카페투어",
                                            profileImageUrl = PROFILE_IMAGE,
                                        ),
                                    rating = 4,
                                    content = "코스 좋아요! 세 번째 카페가 조금 붐볐어요.",
                                    createdAt = kst(2026, 7, 5, 18, 5),
                                    relativeTime = "6일 전",
                                    photoUrls = emptyList(),
                                    tags = listOf(ReviewTagResponse("장소 조합이 좋아요", "combo")),
                                ),
                            ),
                    ),
            )
    }
}

data class CourseScreenResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    /** 작성자가 단 해시태그. [themes](카테고리 단일값)와 별개이며, 순서는 보장하지 않고 없으면 빈 배열. */
    val tags: List<String>,
    val description: String,
    val stats: CourseStatsResponse,
    val author: AuthorResponse,
    val places: List<CoursePlaceScreenResponse>,
    val viewer: CourseViewerResponse,
) {
    companion object {
        fun from(
            course: CourseDetailResult,
            author: UserProfileResult,
            placeSummaries: List<PlaceSummary>,
        ): CourseScreenResponse {
            val placesById = placeSummaries.associateBy { it.id }
            return CourseScreenResponse(
                id = course.id.toString(),
                title = course.title,
                coverImageUrl = course.coverImageUrl,
                themes = course.theme?.let { listOf(it) } ?: emptyList(),
                tags = course.tags,
                description = course.description,
                stats = CourseStatsResponse.from(course),
                author = AuthorResponse.from(author),
                places = course.places.map { CoursePlaceScreenResponse.from(it, placesById[it.placeId]) },
                viewer = CourseViewerResponse(course.hasSaved, course.hasStartedCourse),
            )
        }
    }
}

/** 코스 작성자 카드 — 전체 프로필(팔로우 관계 포함). */
data class AuthorResponse(
    val id: Long,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String,
    val isFollowing: Boolean,
    val isFollower: Boolean,
) {
    companion object {
        fun from(profile: UserProfileResult): AuthorResponse =
            AuthorResponse(
                id = profile.id,
                nickname = profile.nickname,
                handle = profile.handle ?: "",
                profileImageUrl = profile.profileImageUrl ?: "",
                isFollowing = profile.isFollowing,
                isFollower = profile.isFollower,
            )
    }
}

/** 코스 요약 지표. tracingCount 는 따라가기 원시 카운트(축약은 프론트에서). */
data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCount: Int,
) {
    companion object {
        fun from(course: CourseDetailResult): CourseStatsResponse =
            CourseStatsResponse(
                placeCount = course.places.size,
                walkingMinutes = course.places.sumOf { it.walkingMinutesToNext ?: 0 },
                tracingCount = course.tracingsCnt,
            )
    }
}

/**
 * 코스에 담긴 장소(course_places 행) + 장소 도메인 메타(name·categories·location).
 * id 는 course_place 식별자, placeId 는 place 도메인 식별자. caption·images·도보시간은 코스에서,
 * name·categories·location 은 장소 조회([PlaceSummary])에서 온다.
 * 삭제된 장소면 place 요약이 없어 name=null·categories=[]·location=null 이 된다(지도 핀을 못 찍는다).
 */
data class CoursePlaceScreenResponse(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val name: String?,
    val caption: String?,
    val walkingMinutesToNext: Int?,
    val categories: List<String>,
    /** 장소 좌표(지도 핀). 삭제된 장소는 요약이 없어 null. */
    val location: PlaceLocationResponse?,
    val images: List<CoursePlaceImageResponse>,
) {
    companion object {
        fun from(
            place: CoursePlaceResult,
            summary: PlaceSummary?,
        ): CoursePlaceScreenResponse =
            CoursePlaceScreenResponse(
                id = place.id,
                placeId = place.placeId,
                orderNo = place.orderNo,
                name = summary?.name,
                caption = place.caption,
                walkingMinutesToNext = place.walkingMinutesToNext,
                categories = summary?.let { listOf(it.category) } ?: emptyList(),
                location = summary?.let { PlaceLocationResponse(it.latitude, it.longitude) },
                images = place.images.map(CoursePlaceImageResponse::from),
            )
    }
}

data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
) {
    companion object {
        fun from(image: CoursePlaceImageResult): CoursePlaceImageResponse =
            CoursePlaceImageResponse(image.imageUrl, image.orderNo)
    }
}

data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)

/** 리뷰 섹션 — 코스 전체 집계 + 최신순 프리뷰. 전체 목록/페이징은 `/api/v1/courses/{id}/reviews` 로 조회한다. */
data class ReviewSummaryResponse(
    val averageRating: Double,
    val totalCount: Int,
    val hasCompletedCourse: Boolean,
    val ratingDistribution: List<RatingCountResponse>,
    val previews: List<ReviewPreviewResponse>,
)

data class RatingCountResponse(
    val rating: Int,
    val count: Int,
)

data class ReviewPreviewResponse(
    val id: String,
    val author: ReviewAuthorResponse?,
    val rating: Int,
    val content: String,
    val createdAt: OffsetDateTime,
    val relativeTime: String,
    val photoUrls: List<String>,
    val tags: List<ReviewTagResponse>,
)

/** 리뷰 작성자 — 간략 정보(닉네임·프로필 이미지). */
data class ReviewAuthorResponse(
    val id: Long,
    val nickname: String,
    val profileImageUrl: String,
)

data class ReviewTagResponse(
    val label: String,
    val icon: String,
)
