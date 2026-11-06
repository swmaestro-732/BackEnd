package com.example.backend.chat.application.port.inbound

import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.domain.model.ChatMessage

interface ChatMessageUseCase {
    /** 참여하지 않은 방이면 4033. 같은 clientMessageId 로 다시 보내면 기존 메시지를 돌려준다. */
    fun send(command: SendChatMessageCommand): ChatMessage

    /**
     * seq 오름차순 [size] 건. [afterSeq] 가 있으면 그 뒤(놓친 메시지), 없으면 [beforeSeq] 앞(없으면 최신)부터 거슬러 올라간다.
     * 참여하지 않은 방이면 4033.
     */
    fun list(
        roomId: Long,
        userId: Long,
        beforeSeq: Long?,
        afterSeq: Long?,
        size: Int,
    ): ChatMessageSummaryPage
}
