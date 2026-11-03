package com.example.backend.course.application.port.inbound.dto

/** 추천 코스 한 페이지 — 랭킹 순 요약과 다음 페이지용 불투명 커서(없으면 null). */
data class CourseRecommendPage(
    val items: List<CourseSummary>,
    val hasNext: Boolean,
    val nextCursor: String?,
)
