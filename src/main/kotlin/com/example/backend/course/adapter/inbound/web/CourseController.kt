package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CoursePlaceImageResponse
import com.example.backend.course.adapter.inbound.web.response.CoursePlaceResponse
import com.example.backend.course.adapter.inbound.web.response.CourseResponse
import com.example.backend.course.adapter.inbound.web.response.CourseStatsResponse
import com.example.backend.course.adapter.inbound.web.response.CourseViewerResponse
import com.example.backend.course.adapter.inbound.web.response.CreateCourseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스(노션 명세 · Course). **모킹 API**.
 *
 * - [getDetail] 코스 상세(`GET /api/v1/courses/{courseId}`): 컨트롤러에서 목 데이터를 직접 만들어 반환한다.
 *   존재하는 코스는 id=1 뿐이며, 나머지는 404(COURSE_NOT_FOUND).
 * - [create] 코스 생성(`POST /api/v1/courses`): 필드는 디자인 목업(코스 만들기: 코스 정보 →
 *   장소 담기 → 공개 설정)과 courses 스키마에서 도출해 합의했다(노션 명세 필드 미작성 상태).
 *
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseController {
    @GetMapping("/courses/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailResponse> {
        MockErrors.throwIfRequested(mockError)
        if (courseId != 1L) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        return ApiResponse.success(MOCK_DETAIL)
    }

    /**
     * 코스 생성(모킹). 발행(isPublished=true)과 임시저장(false)을 함께 처리한다.
     *
     * 검증
     * - 필드 형식·범위(title·tags·places 등)는 Bean Validation([CreateCourseRequest]) → 400 VALIDATION_FAILED + fieldErrors.
     * - 아래 교차 필드·비즈니스 규칙은 애노테이션으로 표현할 수 없어 직접 검증한다 → 400 INVALID_INPUT.
     *   - 발행 코스는 장소가 1곳 이상이어야 한다(임시저장은 "아직 장소 없음" 허용 — 디자인 임시저장 목록).
     *   - places 의 orderNo 는 중복될 수 없다.
     */
    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CreateCourseResponse> {
        MockErrors.throwIfRequested(mockError)
        validateCreate(request)
        return ApiResponse.success(CreateCourseResponse(courseId = MOCK_COURSE_ID))
    }

    private fun validateCreate(request: CreateCourseRequest) {
        if (request.isPublished && request.places.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "코스를 발행하려면 장소를 1곳 이상 담아야 합니다.")
        }
        if (request.places
                .map { it.orderNo }
                .toSet()
                .size != request.places.size
        ) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "장소 순서(orderNo)가 중복되었습니다.")
        }
    }

    private companion object {
        /** 모킹 고정 id — 코스 상세 목 데이터(courseId=1)와 이어지도록 항상 1을 반환한다. */
        const val MOCK_COURSE_ID = 1L

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
                        themes = listOf("데이트"),
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
