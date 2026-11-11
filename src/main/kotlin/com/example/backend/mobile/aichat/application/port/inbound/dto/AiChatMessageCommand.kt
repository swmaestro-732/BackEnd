package com.example.backend.mobile.aichat.application.port.inbound.dto

import com.example.backend.common.geo.Coordinate

/** 사용자 답변 한 턴. [sessionId] 가 없으면 새 대화(지역 답변)로 본다. CHIP 이면 [value], TEXT 면 [text] 가 있다. */
data class AiChatMessageCommand(
    val userId: Long,
    val sessionId: String?,
    val type: AiChatInputType,
    val value: String?,
    val text: String?,
    val location: Coordinate?,
)

enum class AiChatInputType { CHIP, TEXT }

/** 대화 단계. 응답의 step 으로 그대로 나간다. */
enum class AiChatStep { REGION, THEME, PLACE, PLACE_CONFIRM, RESULT }
