package com.example.backend.chat.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.chat.adapter.inbound.web.request.SendChatMessageRequest
import com.example.backend.chat.adapter.inbound.web.response.ChatMessageListResponse
import com.example.backend.chat.adapter.inbound.web.response.SentChatMessageResponse
import com.example.backend.chat.application.port.inbound.ChatMessageUseCase
import com.example.backend.common.response.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat-rooms/{roomId}/messages")
class ChatMessageController(
    private val chatMessageUseCase: ChatMessageUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun send(
        @PathVariable roomId: Long,
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: SendChatMessageRequest,
    ): ApiResponse<SentChatMessageResponse> =
        ApiResponse.success(SentChatMessageResponse.from(chatMessageUseCase.send(request.toCommand(roomId, userId))))

    @GetMapping
    @AccessTokenRequired
    fun list(
        @PathVariable roomId: Long,
        @CurrentUserId userId: Long,
        @RequestParam(required = false) beforeSeq: Long?,
        @RequestParam(required = false) afterSeq: Long?,
        @RequestParam(defaultValue = "30") @Min(1) @Max(100) size: Int,
    ): ApiResponse<ChatMessageListResponse> =
        ApiResponse.success(
            ChatMessageListResponse.from(chatMessageUseCase.list(roomId, userId, beforeSeq, afterSeq, size)),
        )
}
