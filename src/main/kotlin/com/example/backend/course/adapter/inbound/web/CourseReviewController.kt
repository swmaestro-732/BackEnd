package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseReviewRequest
import com.example.backend.course.adapter.inbound.web.response.CreateCourseReviewResponse
import com.example.backend.course.application.port.inbound.CourseReviewUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 리뷰 작성·삭제(노션 명세 · Course · course-review).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 생성은 `?mock=true` 면 저장 없이 고정 id([CreateCourseReviewResponse.MOCK])를
 * 반환한다(장소 리뷰 선례와 동일 규칙 — 운영 프로파일에서는 [MockGuard] 가 무시한다).
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * **목록 조회는 이 컨트롤러에 없다** — 후기 목록 화면이 작성자 프로필(user)까지 함께 그리는 화면 조합이라
 * BFF([com.example.backend.mobile.course.adapter.inbound.web.CourseReviewScreenController],
 * `GET /service/v1/courses/{courseId}/reviews`)가 담당한다.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
class CourseReviewController(
    private val courseReviewUseCase: CourseReviewUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun create(
        @PathVariable courseId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseReviewRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CreateCourseReviewResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(CreateCourseReviewResponse.MOCK, "리뷰가 등록되었습니다.")
        }

        val review = courseReviewUseCase.create(request.toCommand(courseId, userId))
        return ApiResponse.success(CreateCourseReviewResponse.from(review), "리뷰가 등록되었습니다.")
    }

    /**
     * 코스 리뷰 삭제 — **아직 모킹**. 삭제 없이 고정 성공 메시지만 내려준다.
     * 실구현은 소프트 삭제(deleted_at + status=DELETED)·작성자 본인만·없음/타인 404 은닉(`COURSE_REVIEW_NOT_FOUND`)이다.
     */
    @DeleteMapping("/{reviewId}")
    @AccessTokenRequired
    fun delete(
        @PathVariable courseId: Long,
        @PathVariable reviewId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("리뷰가 삭제되었습니다.")
}
