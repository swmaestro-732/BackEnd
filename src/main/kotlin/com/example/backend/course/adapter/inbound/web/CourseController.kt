package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CreateCourseResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/api/v1")
class CourseController {
    @GetMapping("/courses/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
    ): ApiResponse<CourseDetailResponse> {
        if (courseId != 1L) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "코스를 찾을 수 없습니다: id=$courseId")
        }
        return ApiResponse.success(CourseDetailResponse.mock())
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
    ): ApiResponse<CreateCourseResponse> {
        validateCreate(request)
        return ApiResponse.success(CreateCourseResponse.mock())
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
}
