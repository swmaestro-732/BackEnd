package com.example.backend.mobile.aichat.application.port.outbound

/** 아웃바운드 포트 — 장소 이름으로 후보 검색. 검색 엔진을 못 쓰면 빈 목록(대화를 막지 않는다). */
interface AiChatPlacePort {
    fun searchByName(
        name: String,
        size: Int,
    ): List<AiChatPlace>
}

/** 장소 후보 — 되묻기 칩 문구(이름 · 분류 · 동)에 쓴다. */
data class AiChatPlace(
    val id: Long,
    val name: String,
    val categoryLabel: String,
    val dongName: String?,
)
