package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseReviewRequest
import com.example.backend.course.adapter.inbound.web.response.CreateCourseReviewResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 리뷰 작성·삭제(노션 명세 · Course · course-review). **모킹 API**.
 * 노션 명세서의 "코스 리뷰 생성" 페이지 본문이 비어 있어 필드는 디자인에서 도출했고
 * (밴드 I · 5d 코스 리뷰 작성), 장소 리뷰
 * ([com.example.backend.place.adapter.inbound.web.PlaceReviewController])와 같은 계약으로 맞췄다.
 *
 * **목록 조회는 이 컨트롤러에 없다** — 후기 목록 화면이 작성자 프로필(user)까지 함께 그리는 화면 조합이라
 * BFF([com.example.backend.mobile.course.adapter.inbound.web.CourseReviewScreenController],
 * `GET /service/v1/courses/{courseId}/reviews`)가 담당한다.
 *
 * - [create] 코스 리뷰 생성(`POST /api/v1/courses/{courseId}/reviews`): 요청 형식만 검증하고 저장 없이
 *   고정 id([NEXT_REVIEW_ID])를 반환한다. 작성자 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를
 *   받으므로 유효한 토큰이 있어야 한다(익명 요청은 401).
 * - [delete] 코스 리뷰 삭제(`DELETE /api/v1/courses/{courseId}/reviews/{reviewId}`): 삭제 없이 고정 성공 메시지만
 *   내려준다. **노션 명세서·api-spec 카탈로그에 없고 디자인에도 삭제 UI가 없는 신규 초안**이라 코스 삭제
 *   ([CourseController.delete])·장소 리뷰 삭제의 계약을 그대로 따랐다 — 프론트 확인 후 확정한다.
 *   실구현은 소프트 삭제(course_reviews.deleted_at + status=DELETED)이고, **작성자 본인만** 삭제할 수 있으며
 *   없음·타인 리뷰는 존재를 드러내지 않도록 404 로 은닉한다
 *   (전용 에러 코드 `COURSE_REVIEW_NOT_FOUND` 신규 채번 필요 — 현재는 공통 `NOT_FOUND` 4040 으로 모킹).
 *   성공 시 data 없이 안내 메시지만 내려준다.
 *
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/reviews")
class CourseReviewController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable courseId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseReviewRequest,
    ): ApiResponse<CreateCourseReviewResponse> =
        ApiResponse.success(CreateCourseReviewResponse(reviewId = NEXT_REVIEW_ID), "리뷰가 등록되었습니다.")

    @DeleteMapping("/{reviewId}")
    fun delete(
        @PathVariable courseId: Long,
        @PathVariable reviewId: Long,
        @CurrentUserId userId: Long,
    ): ApiResponse<Nothing?> = ApiResponse.ok("리뷰가 삭제되었습니다.")

    private companion object {
        /** 생성 모킹 고정 id — 목 리뷰(1~6) 다음 번호. */
        const val NEXT_REVIEW_ID = 7L
    }
}
