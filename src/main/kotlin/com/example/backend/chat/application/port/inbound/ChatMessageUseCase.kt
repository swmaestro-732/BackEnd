package com.example.backend.chat.application.port.inbound

import com.example.backend.chat.application.port.inbound.dto.ChatMessageSummaryPage
import com.example.backend.chat.application.port.inbound.dto.SendChatMessageCommand
import com.example.backend.chat.domain.model.ChatMessage
import com.example.backend.chat.domain.model.ChatSystem

interface ChatMessageUseCase {
    /** 참여하지 않은 방이면 4033. 같은 clientMessageId 로 다시 보내면 기존 메시지를 돌려준다. */
    fun send(command: SendChatMessageCommand): ChatMessage

    /** 서버가 남기는 시스템 메시지. 입장 메시지는 들어온 사람에게 읽은 것으로 둔다. */
    fun sendSystem(
        roomId: Long,
        system: ChatSystem,
    ): ChatMessage

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

    /** 읽음 번호를 올리고 결과를 돌려준다. 방의 마지막 seq 를 넘지 않고 되돌아가지 않는다. 참여하지 않은 방이면 4033. */
    fun markRead(
        roomId: Long,
        userId: Long,
        seq: Long,
    ): Long
}
