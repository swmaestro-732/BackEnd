package com.example.backend.mobile.aichat.adapter.inbound.web

import com.example.backend.bootstrap.appversion.AppFeature
import com.example.backend.bootstrap.appversion.RequiresAppFeature
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.aichat.adapter.inbound.web.request.AiChatMessageRequest
import com.example.backend.mobile.aichat.adapter.inbound.web.response.AiChatTurnResponse
import com.example.backend.mobile.aichat.application.port.inbound.AiChatUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 코스 추천 AI 채팅(커미 AI) **화면 조합 API** (BFF). `POST /service/v1/ai-chat/messages`.
 * 사용자의 답변(칩 또는 직접 입력)을 보내고 커미의 다음 턴(다음 질문 또는 코스 추천)을 받는다. 첫 답변은 sessionId 없이 보낸다.
 * 대화 진행은 인바운드 포트([AiChatUseCase])가 담당하고, 컨트롤러는 Request → 포트 호출 → Response 매핑만 한다.
 * 로그인 사용자 전용(`@AccessTokenRequired`). 명세는 노션 API 명세서 「AI 채팅 메시지 전송」(SCRUM-785).
 */
@RequiresAppFeature(AppFeature.AI_CHAT)
@RestController
@RequestMapping("/service/v1/ai-chat")
class AiChatController(
    private val aiChatUseCase: AiChatUseCase,
) {
    @PostMapping("/messages")
    @AccessTokenRequired
    fun sendMessage(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: AiChatMessageRequest,
    ): ApiResponse<AiChatTurnResponse> =
        ApiResponse.success(AiChatTurnResponse.from(aiChatUseCase.sendMessage(request.toCommand(userId))))
}
