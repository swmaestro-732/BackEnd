package com.example.backend.mobile.place.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.mobile.place.adapter.inbound.web.response.NearbyCourseResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceDetailScreenResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewAuthorResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceReviewSummaryResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceScreenResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceViewerResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 장소 상세 **화면 조합 목업 API** (BFF).
 * 장소 정보 + 리뷰 요약/미리보기(작성자) + 저장 여부 + "이 근처 코스"(이 장소를 포함한 코스)를 한 번에 내려준다.
 * 개별 도메인 API(`/api/v1/...`)와 구분해 화면 조합 경로(`/service/v1/...`)로 노출한다(코스 상세 선례와 동일 원칙).
 *
 * `viewer.hasSaved`(user 소관)·"이 근처 코스"(course 소관)는 화면 조합이라 도메인 API가 아닌 여기에 둔다.
 * 기존 [com.example.backend.mobile.place.adapter.inbound.web.CourseDetailScreenController] 와 동일하게
 * 컨트롤러에서 목 데이터를 직접 만들어 반환한다. 실제 구현 시 place + user + course inbound 포트 조합으로 교체한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 * 존재하지 않는 장소(id != 101)는 404(PLACE_NOT_FOUND).
 */
@RestController
@RequestMapping("/service/v1")
class PlaceDetailScreenController {
    @GetMapping("/places/{placeId}")
    fun getScreen(
        @PathVariable placeId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<PlaceDetailScreenResponse> {
        MockErrors.throwIfRequested(mockError)
        if (placeId != 101L) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다: id=$placeId")
        }
        return ApiResponse.success(mockScreen())
    }

    private fun mockScreen() =
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
