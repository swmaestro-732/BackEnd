package com.example.backend.chat.domain.model

/** 공유 시점의 장소·코스 스냅샷. 원본이 바뀌거나 지워져도 그대로 보이고, 최신 정보는 [refId] 로 조회한다. */
data class ChatCard(
    val type: ChatCardType,
    val refId: Long,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
)
