package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.CreateCourseFolderRequest
import com.example.backend.user.adapter.inbound.web.request.SaveCourseRequest
import com.example.backend.user.adapter.inbound.web.response.CourseFolderListResponse
import com.example.backend.user.adapter.inbound.web.response.CreateCourseFolderResponse
import com.example.backend.user.adapter.inbound.web.response.SavedCourseListResponse
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 저장·저장 폴더(노션 명세 · User · user-course).
 *
 * 저장(POST)과 조회(GET)의 경로가 다르다(명세 기준) — 저장은 코스 도메인 액션이라 `/api/v1/courses/save`,
 * 조회는 "내" 저장함이라 `/api/v1/my/saved-courses`. 클래스 레벨 매핑 대신 메서드 레벨 전체 경로로 둔다.
 *
 */
@RestController
class SavedCourseController(
    private val savedCourseUseCase: SavedCourseUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/courses/save")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: SaveCourseRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("코스가 저장되었습니다.")

        savedCourseUseCase.save(userId, request.courseId, request.folderId)
        return ApiResponse.ok("코스가 저장되었습니다.")
    }

    @DeleteMapping("/api/v1/courses/save/{courseId}")
    fun unsave(
        @CurrentUserId userId: Long,
        @PathVariable courseId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.ok("코스 저장을 취소했습니다.")

        savedCourseUseCase.unsave(userId, courseId)
        return ApiResponse.ok("코스 저장을 취소했습니다.")
    }

    @GetMapping("/api/v1/my/saved-courses")
    fun list(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SavedCourseListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(SavedCourseListResponse.mock())

        return ApiResponse.success(
            SavedCourseListResponse.from(
                savedCourseUseCase.getSavedCourses(
                    SavedCoursesCommand(userId = userId, folderId = folderId, cursor = cursor, size = size),
                ),
            ),
        )
    }

    @PostMapping("/api/v1/my/folders")
    @AccessTokenRequired
    @ResponseStatus(HttpStatus.CREATED)
    fun createFolder(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateCourseFolderRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CreateCourseFolderResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            return ApiResponse.success(CreateCourseFolderResponse.mock(), "폴더가 생성되었습니다.")
        }

        val folder = savedCourseUseCase.createFolder(userId, request.name.trim())
        return ApiResponse.success(CreateCourseFolderResponse(folderId = folder.id), "폴더가 생성되었습니다.")
    }

    @GetMapping("/api/v1/my/folders")
    @AccessTokenRequired
    fun listFolders(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseFolderListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseFolderListResponse.mock())

        return ApiResponse.success(CourseFolderListResponse.from(savedCourseUseCase.getFolders(userId)))
    }
}
