package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.Positive

/**
 * 코스 저장 요청(모킹 API) — 웹 어댑터 DTO.
 * 노션 필드 명세 미작성 상태라 디자인(코스 상세 → 저장하기 · 폴더 선택 시트)에서 도출.
 *
 * - courseId: 저장할 코스 id. 경로 변수에서 바디로 옮김(`POST /api/v1/course/save`).
 * - folderId: 저장할 폴더(선택). null/생략이면 "미분류"로 저장한다
 *   (saved_courses.folder_id 는 NULL 허용 — SCRUM-336).
 *   "폴더 만들고 저장"은 폴더 생성 API → 이 API 순서의 2회 호출로 처리한다.
 */
data class SaveCourseRequest(
    @field:Positive val courseId: Long,
    @field:Positive val folderId: Long?,
)
