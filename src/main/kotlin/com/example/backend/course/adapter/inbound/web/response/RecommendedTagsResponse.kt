package com.example.backend.course.adapter.inbound.web.response

/**
 * 코스 생성 시 추천 태그 응답 — 노션 API 명세서(Course · 추천 태그) 합의 스펙.
 * 화면에서 `+ 태그명` 칩으로 노출된다(디자인: 코스 만들기 1단계).
 */
data class RecommendedTagsResponse(
    val tags: List<String>,
)
