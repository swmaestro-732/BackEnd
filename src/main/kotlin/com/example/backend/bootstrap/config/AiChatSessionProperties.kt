package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * AI 채팅 세션 저장소 설정. 세션은 Redis 에 둔다 — 운영은 인스턴스가 여럿(ASG)이라 프로세스 메모리에 두면 요청마다 세션이 어긋난다.
 * [allowInMemory] 는 Redis 가 없는 로컬·CI 에서만 켜는 메모리 폴백 허용 스위치다. 꺼진 채 Redis 가 없으면 대화 요청이 503 으로 실패한다.
 */
@ConfigurationProperties(prefix = "ai-chat.session")
data class AiChatSessionProperties(
    val allowInMemory: Boolean = false,
)
