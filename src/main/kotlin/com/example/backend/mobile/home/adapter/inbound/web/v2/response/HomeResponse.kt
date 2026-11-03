package com.example.backend.mobile.home.adapter.inbound.web.v2.response

import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse
import java.time.Instant
import kotlin.math.roundToInt

/**
 * 웹 응답 DTO — 홈 화면 조합(BFF). 프론트 화면 계약 형태.
 * 헤더 프로필([Profile], 비로그인이면 null), 따라가는 중 코스([TracingCourse], 없으면 null), 추천 코스([Course], 피드 랭킹 상위 5개),
 * 근처 저장 장소([NearbySavedPlace], 거리순 최대 5개)를 내려준다.
 * 목([MOCK]) 응답에만 고정 예시를 채워 프론트가 형태를 확인할 수 있게 한다.
 */
data class HomeResponse(
    val profile: Profile?,
    val tracingCourse: TracingCourse?,
    val recommendedCourses: List<Course>,
    val nearbySavedPlaces: List<NearbySavedPlace>,
) {
    data class Profile(
        val nickname: String,
        val profileImageUrl: String?,
    )

    /** 따라가는 중 코스 카드 — 가장 최근에 시작한 진행 중 세션 1건. 이어가기는 traceId 가 아니라 courseId 로 시작 API 를 다시 부른다. */
    data class TracingCourse(
        val traceId: Long,
        val courseId: Long,
        val title: String,
        val coverImageUrl: String?,
        val area: String?,
        val placeCount: Int,
        val startedAt: Instant,
    )

    /** 코스 카드 한 장 — 홈 추천 코스와 추천 코스 전체보기([RecommendedCoursesResponse])가 함께 쓴다. */
    data class Course(
        val id: Long,
        val title: String,
        val coverImageUrl: String?,
        val theme: String?,
        val area: String?,
        val placeCount: Int,
        /** 코스 구간 도보 시간 합(분). */
        val walkingMinutes: Int,
        val likesCnt: Int,
        val savesCnt: Int,
    ) {
        companion object {
            fun from(course: HomeFeedCourse): Course =
                Course(
                    id = course.id,
                    title = course.title,
                    coverImageUrl = course.coverImageUrl,
                    theme = course.theme,
                    area = course.area,
                    placeCount = course.placeCount,
                    walkingMinutes = course.walkingMinutes,
                    likesCnt = course.likesCnt,
                    savesCnt = course.savesCnt,
                )
        }
    }

    /** 근처 저장 장소 카드 — rating 은 소수 첫째 자리 평균(리뷰 없으면 0.0), 거리는 미터 반올림, 도보는 분. */
    data class NearbySavedPlace(
        val placeId: Long,
        val name: String,
        val imageUrl: String?,
        val category: String,
        val rating: Double,
        val ratingCount: Int,
        val distanceMeters: Int,
        val walkingMinutes: Int,
        val visited: Boolean,
    )

    companion object {
        fun from(result: HomeResult): HomeResponse =
            HomeResponse(
                profile = result.profile?.let { Profile(nickname = it.nickname, profileImageUrl = it.profileImageUrl) },
                tracingCourse =
                    result.tracingCourse?.let {
                        TracingCourse(
                            traceId = it.traceId,
                            courseId = it.courseId,
                            title = it.title,
                            coverImageUrl = it.coverImageUrl,
                            area = it.area,
                            placeCount = it.placeCount,
                            startedAt = it.startedAt,
                        )
                    },
                recommendedCourses = result.recommendedCourses.map(Course::from),
                nearbySavedPlaces =
                    result.nearbySavedPlaces.map {
                        NearbySavedPlace(
                            placeId = it.place.placeId,
                            name = it.place.name,
                            imageUrl = it.place.imageUrl,
                            category = it.place.category,
                            rating = it.place.rating,
                            ratingCount = it.place.ratingCount,
                            distanceMeters = it.place.distanceMeters.roundToInt(),
                            walkingMinutes = it.walkingMinutes,
                            visited = it.place.visited,
                        )
                    },
            )

        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        /** `?mock=true` 폴백 응답 — 시드/DB 없이 프론트가 붙어볼 수 있게 피그마 홈 예시를 고정으로 내려준다. */
        val MOCK: HomeResponse =
            HomeResponse(
                profile = Profile(nickname = "칠삼이", profileImageUrl = null),
                tracingCourse =
                    TracingCourse(
                        traceId = 10,
                        courseId = 3,
                        title = "을지로 노포 골목 투어",
                        coverImageUrl = image("photo-1498654896293-37aacf113fd9"),
                        area = "을지로",
                        placeCount = 5,
                        startedAt = Instant.parse("2026-11-03T05:30:00Z"),
                    ),
                recommendedCourses =
                    listOf(
                        Course(
                            id = 1,
                            title = "비 오는 날 성수 감성 카페 코스",
                            coverImageUrl = image("photo-1554118811-1e0d58224f24"),
                            theme = "DATE",
                            area = "성수",
                            placeCount = 4,
                            walkingMinutes = 25,
                            likesCnt = 128,
                            savesCnt = 342,
                        ),
                        Course(
                            id = 2,
                            title = "연남동 골목 브런치 산책",
                            coverImageUrl = image("photo-1528605248644-14dd04022da1"),
                            theme = "FOOD",
                            area = "연남",
                            placeCount = 3,
                            walkingMinutes = 18,
                            likesCnt = 74,
                            savesCnt = 205,
                        ),
                    ),
                nearbySavedPlaces =
                    listOf(
                        NearbySavedPlace(
                            placeId = 101,
                            name = "어니언 성수",
                            imageUrl = image("photo-1517433670267-08bbd4be890f"),
                            category = "CAFE",
                            rating = 4.8,
                            ratingCount = 212,
                            distanceMeters = 400,
                            walkingMinutes = 6,
                            visited = false,
                        ),
                        NearbySavedPlace(
                            placeId = 102,
                            name = "대림창고",
                            imageUrl = null,
                            category = "CAFE",
                            rating = 4.6,
                            ratingCount = 158,
                            distanceMeters = 600,
                            walkingMinutes = 9,
                            visited = false,
                        ),
                    ),
            )
    }
}
