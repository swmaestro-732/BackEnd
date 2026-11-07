package com.example.backend.mobile.aichat.application.port.outbound.dto

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep

/**
 * 대화 세션 — 현재 단계와 채워진 슬롯, 이미 보여 준 코스. JSON 으로 직렬화해 저장한다.
 * [placeCandidates] 는 PLACE_CONFIRM 에서 내려준 칩 value → 장소 매핑이다(그 단계가 끝나면 비운다).
 */
data class AiChatSession(
    val id: String,
    val userId: Long,
    val step: AiChatStep,
    val region: RegionSlot? = null,
    val theme: ThemeSlot? = null,
    val place: PlaceSlot? = null,
    val placeCandidates: Map<String, PlaceSlot> = emptyMap(),
    val shownCourseIds: List<Long> = emptyList(),
    /** 이 세션에서 LLM 슬롯 추출을 호출한 횟수(세션당 상한용). */
    val llmCalls: Int = 0,
) {
    /** 지역 슬롯 — 표시 이름과 법정동코드 prefix(시도 2·시군구 5·읍면동 10자리 혼재 가능). */
    data class RegionSlot(
        val label: String,
        val prefixes: List<String>,
    )

    /**
     * 테마 슬롯. [category] 가 있으면 그 카테고리로 거르고, 없으면 [likeThemes](여러 사전 일치)·[keyword](사전에 없는 직접 입력)로
     * 가산만 한다. 셋 다 비면 "상관없어요".
     */
    data class ThemeSlot(
        val category: String?,
        val keyword: String?,
        val likeThemes: List<String>,
    )

    data class PlaceSlot(
        val id: Long,
        val name: String,
    )
}
