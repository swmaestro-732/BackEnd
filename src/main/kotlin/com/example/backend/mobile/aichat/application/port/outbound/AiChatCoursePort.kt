package com.example.backend.mobile.aichat.application.port.outbound

import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse

/** 아웃바운드 포트 — 조건에 맞는 추천 코스 조회. course 도메인 추천 랭킹(739·782)에 위임한다. */
interface AiChatCoursePort {
    fun recommend(criteria: AiChatCourseCriteria): List<AiChatCourse>
}

/**
 * 추천 조건. [regionPrefixes] 가 비면 지역 제한 없음, [category] 가 있으면 그 카테고리만.
 * [likeThemes]·[keyword] 는 가산(필터 아님), [placeId] 는 그 장소가 들어간 코스만, [excludeIds] 는 이미 보여 준 코스.
 */
data class AiChatCourseCriteria(
    val regionPrefixes: List<String>,
    val category: String?,
    val likeThemes: List<String>,
    val keyword: String?,
    val placeId: Long?,
    val excludeIds: List<Long>,
    val size: Int,
)
