package com.example.backend.push.application.port.inbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 푸시 발행과 큐에서 수신한 메시지의 발송 계약. */
interface PushUseCase {
    fun request(message: PushMessage)

    fun deliver(message: PushMessage)
}
