package com.example.backend.push.application.port.inbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 다른 도메인이 쓰는 푸시 발행 계약. 메시지 검증 실패는 호출 시점에 IllegalArgumentException 으로 드러난다. */
interface PushUseCase {
    fun request(message: PushMessage)
}
