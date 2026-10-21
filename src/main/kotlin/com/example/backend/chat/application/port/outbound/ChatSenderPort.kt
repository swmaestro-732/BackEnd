package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.application.port.inbound.dto.ChatSender

/** chat 코어는 user 를 직접 알지 않고 ACL 어댑터가 user 인바운드 포트에 위임한다. 탈퇴·미존재 id 는 결과에서 빠진다. */
interface ChatSenderPort {
    fun findSenders(userIds: Collection<Long>): List<ChatSender>
}
