package com.example.backend.chat.domain.model

/** 참여 유형. 현재 위치의 구 방이면 CURRENT, 인접 구 방이면 NEARBY. enum 이름이 DB 저장 계약이다. */
enum class ChatMemberType {
    CURRENT,
    NEARBY,
}
