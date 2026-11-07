package com.example.backend.mobile.aichat.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 아웃바운드 포트 — 좌표가 속한 법정동코드(10자리) 판정. 판정할 수 없으면 null. */
interface AiChatLocationPort {
    fun findAreaCodeAt(location: Coordinate): String?
}
