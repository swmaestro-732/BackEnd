package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.request.EditCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CourseIdResponse
import com.example.backend.course.adapter.inbound.web.response.CoursePlaceImageResponse
import com.example.backend.course.adapter.inbound.web.response.CoursePlaceResponse
import com.example.backend.course.adapter.inbound.web.response.CourseResponse
import com.example.backend.course.adapter.inbound.web.response.CourseStatsResponse
import com.example.backend.course.adapter.inbound.web.response.CourseViewerResponse
import com.example.backend.course.application.port.inbound.CourseUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스(노션 명세 · Course).
 *
 * - [getDetail] 코스 상세(`GET /api/v1/courses/{courseId}`): **실구현** — 인바운드 포트([CourseUseCase])로
 *   DB 조회한다. 시드 데이터가 없는 개발 환경을 위해 `?mock=true` 폴백(기존 [MOCK_DETAIL])을 유지한다.
 * - [create] 코스 생성(`POST /api/v1/courses`): **실구현** — 인바운드 포트([CourseUseCase])로 저장한다.
 *   작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다 — 유효한 토큰이 있어야 동작하며,
 *   경로 자체의 인증 강제(SecurityConfig)는 후속 과제다.
 * - [edit] 코스 편집(`PATCH /api/v1/courses/{courseId}`): **모킹 API** — 실제 저장 없이 받은 courseId 를
 *   그대로 돌려준다. 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 *
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseController(
    private val courseUseCase: CourseUseCase,
) {
    /**
     * 코스 상세 조회. status=ACTIVE·미삭제 코스만 반환하며 PRIVATE 은 소유자만 조회 가능(그 외 404).
     * `?mock=true` 면 DB 조회 없이 고정 목([MOCK_DETAIL])을 반환한다.
     */
    @GetMapping("/courses/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) mock: Boolean = false,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(MOCK_DETAIL)
        return ApiResponse.success(CourseDetailResponse.from(courseUseCase.getDetail(courseId, viewerId)))
    }

    /**
     * 코스 생성. 발행(isPublished=true)과 임시저장(false)을 함께 처리한다.
     *
     * 검증
     * - 필드 형식·범위(title·tags·places 등)는 Bean Validation([CreateCourseRequest]) → 400 VALIDATION_FAILED + fieldErrors.
     * - 교차 필드·비즈니스 규칙(발행 시 장소 1곳 이상, orderNo 중복 금지)은 [CourseUseCase] 가 검증한다 → 400 INVALID_INPUT.
     */
    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseIdResponse> {
        MockErrors.throwIfRequested(mockError)
        val courseId = courseUseCase.create(request.toCommand(userId))
        return ApiResponse.success(CourseIdResponse(courseId = courseId))
    }

    /**
     * 코스 편집(모킹 API). 코스 만들기와 같은 빌더 화면을 재사용하며, 편집한 코스 전체 상태를
     * 되돌려 보내는 전체 치환 계약이다([EditCourseRequest]). 노션 "코스 편집" 페이지는 필드 미작성 상태라
     * 코스 생성 요청과 동일한 필드로 도출했다.
     *
     * 모킹 단계에서는 실제 저장 없이 경로의 `courseId` 를 그대로 응답으로 돌려준다 —
     * 프론트는 편집 후 코스 상세 API 재조회로 화면을 구성한다.
     * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
     */
    @PatchMapping("/courses/{courseId}")
    fun edit(
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EditCourseRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseIdResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(CourseIdResponse(courseId = courseId))
    }

    private companion object {
        fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        fun img(
            token: String,
            orderNo: Int,
        ) = CoursePlaceImageResponse(imageUrl = image(token), orderNo = orderNo)

        /**
         * 코스 상세 목 — 디자인(코스 상세)의 예시 반영. 화면 조합 목([com.example.backend.bff.adapter.inbound.web.CourseDetailScreenController])과
         * 같은 코스(비 오는 날 성수 감성 카페 코스)로 값을 맞춰 두었다. caption 은 장소명.
         */
        val MOCK_DETAIL: CourseDetailResponse =
            CourseDetailResponse(
                course =
                    CourseResponse(
                        id = "1",
                        title = "비 오는 날 성수 감성 카페 코스",
                        coverImageUrl = image("THb4AHDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                        theme = "데이트",
                        description =
                            "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, " +
                                "장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                        stats =
                            CourseStatsResponse(
                                placeCount = 4,
                                walkingMinutes = 20,
                                tracingCountLabel = "1.2k",
                            ),
                        authorId = 1L,
                        places =
                            listOf(
                                CoursePlaceResponse(
                                    id = 1L,
                                    placeId = 101L,
                                    orderNo = 1,
                                    caption = "어니언 성수",
                                    walkingMinutesToNext = 6,
                                    images =
                                        listOf(
                                            img("THIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA", 0),
                                            img("Qri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw", 1),
                                        ),
                                ),
                                CoursePlaceResponse(
                                    id = 2L,
                                    placeId = 102L,
                                    orderNo = 2,
                                    caption = "대림창고 갤러리",
                                    walkingMinutesToNext = 3,
                                    images = listOf(img("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 3L,
                                    placeId = 103L,
                                    orderNo = 3,
                                    caption = "센터커피 성수",
                                    walkingMinutesToNext = 5,
                                    images = listOf(img("TMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 4L,
                                    placeId = 104L,
                                    orderNo = 4,
                                    caption = "카페 할아버지공장",
                                    walkingMinutesToNext = null,
                                    images =
                                        listOf(
                                            img("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA", 0),
                                            img("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw", 1),
                                        ),
                                ),
                            ),
                        viewer =
                            CourseViewerResponse(
                                hasSaved = false,
                                hasStartedCourse = false,
                            ),
                    ),
            )
    }
}
