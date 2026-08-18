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
 * 노션 명세가 액션 경로(`/api/v1/courses/save`)를 리소스 경로(`/api/v1/saved-courses`)로 바꿨다.
 * 클래스 레벨 매핑 대신 메서드 레벨 전체 경로로 둔다.
 *
 * **저장·취소는 경로 두 개를 한 핸들러에 매핑한다** — 프론트가 이미 구 경로로 연동을 마쳤기 때문에(노션
 * `프론트 구현 여부 = API 연동 완료`) 한 번에 끊을 수 없다. 신 경로를 앞에 두고, 구 경로는 노션 명세의
 * "(삭제 예정)" 표기대로 나중에 지운다 — 삭제 조건은 "프론트 코드 반영"이 아니라 **구 경로 호출량 0**이다
 * (`http_server_requests_seconds_count{uri="/api/v1/courses/save"}` 로 확인한다. 한 핸들러에 두 경로를
 * 매핑해도 메트릭 uri 태그는 매칭된 패턴별로 따로 잡힌다).
 * 로직이 갈라지지 않도록 핸들러는 하나만 두고 매핑만 둘로 둔다.
 *
 * 폴더(`/api/v1/folders`)는 프론트 연동 전이라(노션 `시작 전`) 구 경로(`/api/v1/my/folders`) 없이 바로 옮긴다.
 *
 * 신 경로들은 `/api/v1/my` 밖이라 SecurityConfig 에 인증 필수 경로로 따로 등록했다 —
 * 안 그러면 `/api` 하위 permitAll 에 걸려 폴더 API 가 무인증으로 열린다.
 */
@RestController
class SavedCourseController(
    private val savedCourseUseCase: SavedCourseUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/saved-courses", "/api/v1/courses/save") // 구 경로: 호출량 0 확인 후 제거
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

    @DeleteMapping("/api/v1/saved-courses/{courseId}", "/api/v1/courses/save/{courseId}") // 구 경로: 호출량 0 확인 후 제거
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

    @PostMapping("/api/v1/folders")
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

        val folder = savedCourseUseCase.createFolder(userId, request.name)
        return ApiResponse.success(CreateCourseFolderResponse(folderId = folder.id), "폴더가 생성되었습니다.")
    }

    @GetMapping("/api/v1/folders")
    @AccessTokenRequired
    fun listFolders(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<CourseFolderListResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(CourseFolderListResponse.mock())

        return ApiResponse.success(CourseFolderListResponse.from(savedCourseUseCase.getFolders(userId)))
    }
}
