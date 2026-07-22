package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.NotNull

/**
 * 코스 저장 요청(모킹 API) — 웹 어댑터 DTO.
 * 노션 필드 명세 미작성 상태라 디자인(코스 상세 → 저장하기 · 폴더 선택 시트)에서 도출 —
 * 항상 폴더를 골라 "○○에 저장"한다(saved_courses.folder_id NOT NULL).
 * "폴더 만들고 저장"은 폴더 생성 API → 이 API 순서의 2회 호출로 처리한다.
 */
data class SaveCourseRequest(
    @field:NotNull
    val folderId: Long?,
)
