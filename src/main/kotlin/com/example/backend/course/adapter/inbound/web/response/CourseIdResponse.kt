package com.example.backend.course.adapter.inbound.web.response

/**
 * 코스 식별자만 담는 응답 — 생성·편집이 공유한다.
 * 두 경우 모두 이후 화면은 코스 상세 API 재조회로 구성하므로 courseId 만 내려준다.
 */
data class CourseIdResponse(
    val courseId: Long,
)
