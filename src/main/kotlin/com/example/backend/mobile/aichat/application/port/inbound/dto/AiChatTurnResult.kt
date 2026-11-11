package com.example.backend.mobile.aichat.application.port.inbound.dto

import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse

/**
 * 커미의 한 턴 — 말풍선 문구, 고를 수 있는 칩, 입력창 상태, 추천 코스(RESULT 에서만).
 * REGION 으로 되돌아갈 때 [quickReplies] 는 비어 있고 앱이 자기 지역 칩을 다시 보여준다.
 */
data class AiChatTurnResult(
    val sessionId: String,
    val step: AiChatStep,
    val messages: List<String>,
    val quickReplies: List<AiChatQuickReply>,
    val inputEnabled: Boolean,
    val inputPlaceholder: String?,
    val courses: List<AiChatCourse>,
)

data class AiChatQuickReply(
    val label: String,
    val value: String,
    val requiresLocation: Boolean = false,
)
