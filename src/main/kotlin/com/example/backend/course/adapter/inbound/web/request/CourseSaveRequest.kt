package com.example.backend.course.adapter.inbound.web.request

/**
 * 코스 저장 요청(모킹 API) — 웹 어댑터 DTO.
 * RESTful 리소스 경로(`POST /api/v1/courses/{courseId}/save`)용. 구 경로의
 * `user.adapter...SaveCourseRequest` 와 동일 필드지만, 도메인 경계(course→user.adapter 의존 금지)상 course 로컬로 둔다.
 *
 * - folderId: 저장할 폴더(선택). null/생략이면 "미분류"로 저장한다(saved_courses.folder_id NULL 허용 — SCRUM-336).
 */
data class CourseSaveRequest(
    val folderId: Long?,
)
