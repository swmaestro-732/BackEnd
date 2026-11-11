package com.example.backend.mobile.aichat.application.port.outbound

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatStep

/**
 * 아웃바운드 포트 — 자유 입력 문장을 슬롯 값으로 바꾼다(LLM). 역할은 변환뿐이며 검색·검증은 호출자가 한다.
 * LLM 이 꺼져 있거나 시간 안에 답하지 못하면 null — 호출자는 규칙 기반 해석으로 진행한다.
 */
interface AiChatSlotExtractionPort {
    fun extract(request: SlotExtractionRequest): ExtractedSlots?
}

/** 현재 질문([step])과 사용자의 문장. 이미 채워진 슬롯은 참고용으로 넘긴다(다시 묻지 않게). */
data class SlotExtractionRequest(
    val step: AiChatStep,
    val text: String,
    val regionLabel: String?,
    val themeCategory: String?,
)

/**
 * 추출 결과. [themes] 는 코스 카테고리 12종 이름만(호출자가 다시 검증), 12종에 안 맞는 표현은 [themeKeywords] 로.
 * 비어 있는 값은 "문장에 없음"이다. [offTopic] 이면 코스 찾기와 무관한 말이라 같은 질문을 다시 낸다.
 */
data class ExtractedSlots(
    val regionText: String?,
    val nearMe: Boolean,
    val themes: List<String>,
    val themeKeywords: List<String>,
    val placeName: String?,
    val noPlace: Boolean,
    val offTopic: Boolean,
)
