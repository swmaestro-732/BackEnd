package com.example.backend.chat.adapter.inbound.web.response

import com.example.backend.chat.domain.model.ChatCard
import com.example.backend.chat.domain.model.ChatCardType

/** 공유 시점 스냅샷. 누르면 [refId] 로 장소·코스 상세를 조회한다. */
data class ChatCardResponse(
    val type: ChatCardType,
    val refId: Long,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(card: ChatCard) = ChatCardResponse(card.type, card.refId, card.title, card.subtitle, card.thumbnailUrl)
    }
}
