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
 * - [save] 코스 저장(`POST /api/v1/saved-courses`): **실구현** — 인바운드 포트([SavedCourseUseCase])로 저장한다.
 *   folderId 를 주면 소유권을 검증하고(그 외 400), 이미 저장한 코스면 중복 저장으로 막는다(409). 코스당 저장 레코드는 1개다.
 *   저장 주체 식별이 필요해 `@CurrentUserId`(JWT subject)로 userId 를 받는다.
 * - [unsave] 코스 저장 취소(`DELETE /api/v1/saved-courses/{courseId}`): **실구현** — 저장의 역연산이라
 *   courseId 로 (user, course) 저장 레코드를 지운다. 저장돼 있지 않아도 오류 없이 성공한다(멱등 — unfollow 선례와 동일).
 * - [list] 저장 코스 조회(`GET /api/v1/my/saved-courses`): **실구현** — 인바운드 포트([SavedCourseUseCase])로 조회한다.
 *   저장 레코드(ID 위주)를 최신 저장순으로 커서 페이지네이션해 반환한다 —
 *   코스 요약·완주 여부는 화면 조합 API(`GET /service/v1/my/saved-courses`)가 담당한다.
 *   (노션에 이 행이 없어 경로를 아직 못 옮겼다 — 저장·취소만 리소스 경로로 갔다.)
 * - [createFolder]·[listFolders] 저장 폴더(`POST`·`GET /api/v1/folders`): **실구현**.
 *
 * **저장·취소는 경로 두 개를 한 핸들러에 매핑한다** — 프론트가 이미 구 경로로 연동을 마쳤기 때문에(노션
 * `프론트 구현 여부 = API 연동 완료`) 한 번에 끊을 수 없다. 신 경로를 앞에 두고, 구 경로는 노션 명세의
 * "(삭제 예정)" 표기대로 나중에 지운다 — 삭제 조건은 "프론트 코드 반영"이 아니라 **구 경로 호출량 0**이다
 * (`http_server_requests_seconds_count{uri="/api/v1/courses/save"}` 로 확인한다. 한 핸들러에 두 경로를
 * 매핑해도 메트릭 uri 태그는 매칭된 패턴별로 따로 잡힌다).
 * 로직이 갈라지지 않도록 핸들러는 하나만 두고 매핑만 둘로 둔다.
 * 폴더(`/api/v1/folders`)는 프론트 연동 전이라(노션 `시작 전`) 구 경로 없이 바로 옮겼다.
 *
 * 신 경로들은 `/api/v1/my` 밖이라 SecurityConfig 에 인증 필수 경로로 따로 등록했다 —
 * 안 그러면 `/api` 하위 permitAll 에 걸려 무인증으로 열린다.
 * 그에 더해 "나" 기준 쓰기 액션에는 [AccessTokenRequired] 메서드 시큐리티로 access 토큰을 강제한다 —
 * 회원가입 목적 토큰(purpose != access)으로는 남의 저장함에 쓸 수 없다(UserController 선례와 동일).
 *
 * 시드/DB 없이 프론트가 붙어볼 수 있도록 `?mock=true` 면 저장/조회 없이 고정 목([SavedCourseListResponse.mock])을
 * 반환한다(코스 상세 선례와 동일 규칙). 모킹 에러(`?mockError=<code>`)는 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 */
@RestController
class SavedCourseController(
    private val savedCourseUseCase: SavedCourseUseCase,
    private val mockGuard: MockGuard,
) {
    @PostMapping("/api/v1/saved-courses", "/api/v1/courses/save") // 구 경로: 호출량 0 확인 후 제거
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
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
    @AccessTokenRequired
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
