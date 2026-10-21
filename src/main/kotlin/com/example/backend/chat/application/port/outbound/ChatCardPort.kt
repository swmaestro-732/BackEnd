package com.example.backend.chat.application.port.outbound

import com.example.backend.chat.domain.model.ChatCard
import com.example.backend.chat.domain.model.ChatCardType

/** chat 코어는 place·course 를 직접 알지 않고 ACL 어댑터가 각 인바운드 포트에 위임한다. */
interface ChatCardPort {
    /** [viewerId] 가 공유할 수 있는 장소·코스의 카드. 없거나 공유할 수 없으면 null. */
    fun findCard(
        type: ChatCardType,
        refId: Long,
        viewerId: Long,
    ): ChatCard?
}
