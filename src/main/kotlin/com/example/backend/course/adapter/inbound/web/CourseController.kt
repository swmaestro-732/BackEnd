package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CreateCourseRequest
import com.example.backend.course.adapter.inbound.web.request.DuplicateCourseRequest
import com.example.backend.course.adapter.inbound.web.request.EditCourseRequest
import com.example.backend.course.adapter.inbound.web.response.CourseDetailResponse
import com.example.backend.course.adapter.inbound.web.response.CourseIdResponse
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseUseCase
import com.example.backend.course.application.port.inbound.dto.CourseSummary
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 인바운드 어댑터 — 코스(노션 명세 · Course).
 * 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RequiresAppFeature(AppFeature.COURSE_DETAIL)
@RestController
@RequestMapping("/api/v1/courses")
class CourseController(
    private val courseUseCase: CourseUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val mockGuard: MockGuard,
) {
    /**
     * 로그인 작성자의 임시저장 코스를 최근 수정순으로 조회한다. 공개범위는 본인 목록이라 적용하지 않는다.
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 조회 없이 고정 목록을 반환한다.
     */
    @RequiresAppFeature(AppFeature.COURSE_CREATE)
    @GetMapping("/drafts")
    fun listDrafts(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<List<CourseSummary>> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(DRAFTS_MOCK)
        return ApiResponse.success(courseQueryUseCase.listDraftsByAuthor(userId))
    }

    /**
     * 코스 상세 조회. status=ACTIVE·미삭제 코스만 반환하며 PRIVATE 은 소유자만 조회 가능(그 외 404).
     * `?mock=true` 이고 [MockGuard] 가 모킹을 허용할 때만 DB 조회 없이 고정 목([CourseDetailResponse.MOCK])을
     * 반환하고, 그 외에는 정상 유스케이스 경로를 탄다.
     */
    @GetMapping("/{courseId}")
    fun getDetail(
        @PathVariable courseId: Long,
        @CurrentUserId viewerId: Long?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseDetailResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseDetailResponse.MOCK)
        return ApiResponse.success(CourseDetailResponse.from(courseQueryUseCase.getDetail(courseId, viewerId)))
    }

    /** 코스 생성. 발행(isPublished=true)과 임시저장(false)을 함께 처리한다. */
    @RequiresAppFeature(AppFeature.COURSE_CREATE)
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.create(request.toCommand(userId))
        return ApiResponse.success(CourseIdResponse.from(course))
    }

    /** 코스 수정. */
    @PatchMapping("/{courseId}")
    fun edit(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: EditCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)
        val course = courseUseCase.edit(request.toCommand(userId, courseId))
        return ApiResponse.success(CourseIdResponse.from(course))
    }

    /** 코스 복제 */
    @PostMapping("/{courseId}/duplicates")
    @ResponseStatus(HttpStatus.CREATED)
    fun duplicate(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @Valid @RequestBody request: DuplicateCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseIdResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseIdResponse.MOCK)

        val course = courseUseCase.duplicate(request.toCommand(userId, courseId))
        return ApiResponse.success(CourseIdResponse.from(course))
    }

    /** 코스 삭제(소프트 삭제) */
    @DeleteMapping("/{courseId}")
    fun delete(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("코스가 삭제되었습니다.")
        courseUseCase.delete(userId, courseId)
        return ApiResponse.ok("코스가 삭제되었습니다.")
    }

    private companion object {
        val DRAFTS_MOCK: List<CourseSummary> =
            listOf(
                CourseSummary(
                    id = 2,
                    authorId = 1,
                    title = "비 오는 날 성수 감성 카페 코스",
                    coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600",
                    theme = null,
                    area = null,
                    likesCnt = 0,
                    savesCnt = 0,
                    createdAt = Instant.parse("2026-07-20T02:30:00Z"),
                ),
            )
    }
}
