package com.example.backend.course.application.port.inbound.dto

/**
 * 추천 조회 조건. [likeThemes] 는 코스 카테고리 이름, [areaCodes] 는 10자리 법정동코드(시군구 선택은 뒤 5자리 00000) —
 * 비로그인이면 둘 다 빈 목록(관심 항 없이 최신성·저장수만). [category] 는 전체보기의 선택 필터(알 수 없는 이름이면 400).
 */
data class CourseRecommendQuery(
    val likeThemes: List<String>,
    val areaCodes: List<String>,
    val cursor: String?,
    val size: Int,
    val category: String? = null,
)
