package com.example.backend.course.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.course.adapter.inbound.web.request.CourseSaveRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 저장(RESTful 리소스 경로). **모킹 API**.
 *
 * 코스 리소스에 대한 액션이므로 `POST /api/v1/courses/{courseId}/save` 로 노출한다.
 * 구 경로 `POST /api/v1/my/saved-courses/{courseId}`([com.example.backend.user.adapter.inbound.web.SavedCourseController.save])
 * 와 동일한 모킹 성공 엔벨로프를 반환한다(신·구 병행 유지). 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체한다.
 * CourseController 의 기존 매핑은 건드리지 않는다.
 */
@RestController
@RequestMapping("/api/v1/courses")
class CourseSaveController {
    @PostMapping("/{courseId}/save")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @PathVariable courseId: Long,
        @RequestBody(required = false) request: CourseSaveRequest? = null,
    ): ApiResponse<Nothing?> = ApiResponse.ok("코스가 저장되었습니다.")
}
