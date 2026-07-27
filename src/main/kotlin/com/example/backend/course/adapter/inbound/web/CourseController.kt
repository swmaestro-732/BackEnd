package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.request.EditCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CourseIdResponse
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
 *   DB 조회한다. 시드 데이터가 없는 개발 환경을 위해 `?mock=true` 폴백([CourseDetailResponse.MOCK])을 유지한다.
 * - [create] 코스 생성(`POST /api/v1/courses`): **실구현** — 인바운드 포트([CourseUseCase])로 저장한다.
 *   작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다 — 유효한 토큰이 있어야 동작하며,
 *   경로 자체의 인증 강제(SecurityConfig)는 후속 과제다. 시드/DB 없이 프론트가 붙어볼 수 있도록
 *   `?mock=true` 면 저장 없이 고정 목([CourseIdResponse.MOCK], 코스 상세 목과 이어짐)을 반환한다.
 * - [edit] 코스 편집(`PATCH /api/v1/courses/{courseId}`): **모킹 API** — 실제 저장 없이 받은 courseId 를
 *   그대로 돌려준다. 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 *
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1/courses")
class CourseController(
    private val courseUseCase: CourseUseCase,
) {
    /**
     * 코스 상세 조회. status=ACTIVE·미삭제 코스만 반환하며 PRIVATE 은 소유자만 조회 가능(그 외 404).
     * `?mock=true` 면 DB 조회 없이 고정 목([CourseDetailResponse.MOCK])을 반환한다.
     */
    @GetMapping("/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) mock: Boolean = false,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(CourseDetailResponse.MOCK)
        return ApiResponse.success(CourseDetailResponse.from(courseUseCase.getDetail(courseId, viewerId)))
    }

    /**
     * 코스 생성. 발행(isPublished=true)과 임시저장(false)을 함께 처리한다.
     *
     * 검증
     * - 필드 형식·범위(title·tags·places 등)는 Bean Validation([CreateCourseRequest]) → 400 VALIDATION_FAILED + fieldErrors.
     * - 교차 필드·비즈니스 규칙(발행 시 장소 1곳 이상, orderNo 중복 금지)은 [CourseUseCase] 가 검증한다 → 400 INVALID_INPUT.
     *
     * `?mock=true` 면 DB 저장 없이 고정 목([CourseIdResponse.MOCK])을 반환한다.
     */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseIdResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.create(request.toCommand(userId))
        return ApiResponse.success(CourseIdResponse(courseId = requireNotNull(course.id)))
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
    @PatchMapping("/{courseId}")
    fun edit(
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EditCourseRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseIdResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(CourseIdResponse(courseId = courseId))
    }
}
