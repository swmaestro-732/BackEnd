package com.example.backend.course.application.port.inbound.dto

/** 검색 결과 — 요약 목록([CourseSummary], 인바운드 공개 계약)과 다음 커서. */
data class CourseSearchResult(
    val courses: List<CourseSummary>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
