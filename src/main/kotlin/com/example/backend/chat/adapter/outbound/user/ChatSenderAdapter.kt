package com.example.backend.chat.adapter.outbound.user

import com.example.backend.chat.application.port.inbound.dto.ChatSender
import com.example.backend.chat.application.port.outbound.ChatSenderPort
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Component

@Component
class ChatSenderAdapter(
    private val userSummaryUseCase: UserSummaryUseCase,
) : ChatSenderPort {
    override fun findSenders(userIds: Collection<Long>): List<ChatSender> =
        userSummaryUseCase.findSummaries(userIds).map {
            ChatSender(userId = it.id, nickname = it.nickname, profileImageUrl = it.profileImageUrl)
        }
}
