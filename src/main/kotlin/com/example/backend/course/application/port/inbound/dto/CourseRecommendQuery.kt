package com.example.backend.course.application.port.inbound.dto

/**
 * 추천 조회 조건. [likeThemes] 는 코스 카테고리 이름, [areaCodes] 는 10자리 법정동코드(시군구 선택은 뒤 5자리 00000) —
 * 비로그인이면 둘 다 빈 목록(관심 항 없이 최신성·저장수만). [category]·[region] 은 전체보기의 선택 필터(알 수 없는 값이면 400) —
 * [region] 은 법정동코드 prefix(시도 2자리·구 있는 시 4자리·시군구 5자리, 광주 `29`·전남 `46` 은 가상 시도).
 */
data class CourseRecommendQuery(
    val likeThemes: List<String>,
    val areaCodes: List<String>,
    val cursor: String?,
    val size: Int,
    val category: String? = null,
    val region: String? = null,
)
