package com.example.backend.mobile.course.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.mobile.course.adapter.inbound.web.response.AuthorResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseDetailScreenResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CoursePlaceImageResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CoursePlaceScreenResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseScreenResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseStatsResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.CourseViewerResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.RatingCountResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.ReviewAuthorResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.ReviewPreviewResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.ReviewSummaryResponse
import com.example.backend.mobile.course.adapter.inbound.web.response.ReviewTagResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 코스 상세 **화면 조합 목업 API** (BFF).
 * 코스 상세 + 작성자 프로필 + 리뷰 요약/프리뷰 + 장소 이름·카테고리를 한 번에 내려준다.
 * 개별 도메인 API(`/api/v1/...`)와 구분해 화면 조합 경로(`/service/v1/...`)로 노출한다.
 *
 * 기존 [com.example.backend.place.adapter.inbound.web.PlaceController] 와 동일하게
 * 컨트롤러에서 목 데이터를 직접 만들어 반환한다. 실제 구현 시 도메인 inbound 포트 조합으로 교체한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 * 존재하지 않는 코스(id != 1)는 404(COURSE_NOT_FOUND).
 */
@RestController
@RequestMapping("/service/v1")
class CourseDetailScreenController {
    @GetMapping("/courses/{courseId}")
    fun getScreen(
        @PathVariable courseId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailScreenResponse> {
        MockErrors.throwIfRequested(mockError)
        if (courseId != 1L) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        return ApiResponse.success(mockScreen())
    }

    private fun mockScreen() =
        CourseDetailScreenResponse(
            course =
                CourseScreenResponse(
                    id = "1",
                    title = "비 오는 날 성수 감성 카페 코스",
                    coverImageUrl = image("HDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                    themes = listOf("데이트"),
                    description = "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, 장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                    stats = CourseStatsResponse(placeCount = 4, walkingMinutes = 20, tracingCountLabel = "1.2k"),
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
                                images = listOf(img("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw", 0)),
                            ),
                            CoursePlaceScreenResponse(
                                id = 3,
                                placeId = 103,
                                orderNo = 2,
                                name = "센터커피 성수",
                                caption = "원두 향이 좋아요. 2층 창가 추천.",
                                walkingMinutesToNext = 5,
                                categories = listOf("카페"),
                                images = listOf(img("TMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw", 0)),
                            ),
                            CoursePlaceScreenResponse(
                                id = 4,
                                placeId = 104,
                                orderNo = 3,
                                name = "카페 할아버지공장",
                                caption = "마무리로 딱. 넓어서 웨이팅 걱정 없어요.",
                                walkingMinutesToNext = null,
                                categories = listOf("카페", "베이커리"),
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

    private companion object {
        const val PROFILE_IMAGE =
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQi7ZSFKA2brmDYt72J8vLDQxgOJKxs-lj4tavhXo_pEA&s=10"

        fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        fun img(
            token: String,
            orderNo: Int,
        ) = CoursePlaceImageResponse(image(token), orderNo)

        fun kst(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
        ) = OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(9))
    }
}
