package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.SaveCourseRequest
import com.example.backend.user.adapter.inbound.web.response.SavedCourseListResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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
 * 인바운드 어댑터 — 저장 코스(노션 명세 · User · user-course). **모킹 API**.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다. 모킹 에러(`?mockError=<code>`)는
 * 전역 아스펙트([com.example.backend.bootstrap.mock.MockErrorAspect])가 주입한다.
 *
 * 경로: `/service/v1/saved-courses` 가 정식이며 `/api/v1/my/saved-courses` 는 deprecated 별칭(동일 핸들러).
 * save 의 RESTful 정식 경로는 `POST /api/v1/courses/{courseId}/save` 다.
 */
@RestController
@RequestMapping(value = ["/service/v1/saved-courses", "/api/v1/my/saved-courses"])
class SavedCourseController {
    /**
     * Deprecated: 코스 리소스 액션이므로 `POST /api/v1/courses/{courseId}/save`
     * ([com.example.backend.course.adapter.inbound.web.CourseSaveController.save]) 로 대체한다.
     * 신·구 병행 유지 중 — 클라이언트 전환 완료 후 제거한다.
     */
    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @PathVariable courseId: Long,
        @Valid @RequestBody request: SaveCourseRequest,
    ): ApiResponse<Nothing?> = ApiResponse.ok("코스가 저장되었습니다.")

    /**
     * 저장 코스 조회(모킹). 항상 [SavedCourseListResponse.mock] 전체를 최신 저장순 **고정 응답**으로 내려준다 —
     * 쿼리 파라미터는 API 계약 확인용으로 받기만 하고 동작(필터·페이지네이션)은 실구현에서 지원한다.
     *
     * 쿼리 파라미터
     * - folderId: 폴더 칩 필터. 생략 시 전체.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<SavedCourseListResponse> = ApiResponse.success(SavedCourseListResponse.mock())
}
