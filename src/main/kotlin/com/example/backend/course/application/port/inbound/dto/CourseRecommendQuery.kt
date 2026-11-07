package com.example.backend.course.application.port.inbound.dto

/**
 * 추천 조회 조건. [likeThemes] 는 코스 카테고리 이름, [areaCodes] 는 10자리 법정동코드(시군구 선택은 뒤 5자리 00000) —
 * 비로그인이면 둘 다 빈 목록(관심 항 없이 최신성·저장수만). [category]·[region] 은 전체보기의 선택 필터(알 수 없는 값이면 400) —
 * [region] 은 법정동코드 prefix(시도 2자리·구 있는 시 4자리·시군구 5자리, 광주 `29`·전남 `46` 은 가상 시도).
 * [regionPrefixes] 는 [region] 없이 임의 길이 prefix 를 직접 거는 필터(AI 채팅의 동 단위 지역). 둘 다 있으면 [region] 이 우선.
 * [placeId] 가 있으면 그 장소가 들어간 코스만, [excludeIds] 는 결과에서 뺄 코스(이미 보여 준 것), [keyword] 는 제목·설명 일치 가산(필터 아님).
 */
data class CourseRecommendQuery(
    val likeThemes: List<String>,
    val areaCodes: List<String>,
    val cursor: String?,
    val size: Int,
    val category: String? = null,
    val region: String? = null,
    val regionPrefixes: List<String> = emptyList(),
    val placeId: Long? = null,
    val excludeIds: List<Long> = emptyList(),
    val keyword: String? = null,
)
