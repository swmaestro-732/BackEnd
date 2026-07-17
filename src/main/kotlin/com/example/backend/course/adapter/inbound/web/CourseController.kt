package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CreateCourseRequest
import com.example.backend.course.application.port.inbound.dto.CreateCourseResponse
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
 * 인바운드 어댑터 — 코스(노션 명세 · Course).
 *
 * - [getDetail] 코스 상세(`GET /api/v1/courses/{courseId}`): 실구현. HTTP 요청을 인바운드 포트([CourseUseCase])
 *   호출로 변환하고, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 * - [create] 코스 생성(`POST /api/v1/courses`): **모킹 API**. 필드는 디자인 목업(코스 만들기: 코스 정보 →
 *   장소 담기 → 공개 설정)과 courses 스키마에서 도출해 합의했다(노션 명세 필드 미작성 상태).
 *   실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 *
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1")
class CourseController(
    private val courseUseCase: CourseUseCase,
) {
    @GetMapping("/courses/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<CourseDetailResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(CourseDetailResponse.from(courseUseCase.getDetail(courseId)))
    }

    /**
     * 코스 생성(모킹). 발행(isPublished=true)과 임시저장(false)을 함께 처리한다.
     *
     * 검증
     * - 필드 형식·범위는 Bean Validation([CreateCourseRequest]) → 400 + fieldErrors.
     * - 발행 코스는 장소가 1곳 이상이어야 한다(임시저장은 "아직 장소 없음" 허용 — 디자인 임시저장 목록).
     * - places 의 orderNo 는 중복될 수 없다.
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
        if (request.tags.any { it.isBlank() || it.length > MAX_TAG_LENGTH }) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "태그는 비어 있을 수 없고 ${MAX_TAG_LENGTH}자 이하여야 합니다.")
        }
    }

    private companion object {
        /** tags 테이블 name varchar(50) 과 동일한 제한. */
        const val MAX_TAG_LENGTH = 50

        /** 모킹 고정 id — 코스 상세 목 데이터(courseId=1)와 이어지도록 항상 1을 반환한다. */
        const val MOCK_COURSE_ID = 1L
    }
}
