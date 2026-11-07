package com.example.backend.mobile.aichat.application.port.inbound

import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatMessageCommand
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatTurnResult

/**
 * 인바운드 포트 — 코스 추천 AI 채팅(커미 AI). 사용자의 답변 한 턴을 받아 다음 질문 또는 추천 결과를 돌려준다.
 * 질문은 지역 → 테마 → 가고 싶은 장소 순으로 고정이고, 세션은 첫 답변에서 만들어 30분 보관한다.
 */
interface AiChatUseCase {
    fun sendMessage(command: AiChatMessageCommand): AiChatTurnResult
}
