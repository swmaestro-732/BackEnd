package com.example.backend.mobile.aichat.application.port.outbound

import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatSession

/** 아웃바운드 포트 — 대화 세션 저장소. 저장할 때마다 보관 기한(30분)을 다시 센다. */
interface AiChatSessionPort {
    fun find(sessionId: String): AiChatSession?

    fun save(session: AiChatSession)
}
