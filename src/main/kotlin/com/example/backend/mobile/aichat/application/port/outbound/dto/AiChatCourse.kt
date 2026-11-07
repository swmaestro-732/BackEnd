package com.example.backend.mobile.aichat.application.port.outbound.dto

/** 추천 코스 카드 한 장 — 홈 추천 코스 카드와 같은 필드(BFF 격리 사본). */
data class AiChatCourse(
    val id: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val area: String?,
    val placeCount: Int,
    val walkingMinutes: Int,
    val likesCnt: Int,
    val savesCnt: Int,
)
