package com.example.backend.user.adapter.inbound.web.request

import jakarta.validation.constraints.Positive

/**
 * 장소 저장 요청 — 웹 어댑터 DTO.
 *
 * - placeId: 저장할 장소 id. 경로 변수에서 바디로 옮김(`POST /api/v1/saved-places` — 코스 저장
 *   `SaveCourseRequest.courseId` 와 동일 컨벤션, 2026-07-28 경로 이동 결정 반영).
 */
data class SavePlaceRequest(
    @field:Positive val placeId: Long,
)
