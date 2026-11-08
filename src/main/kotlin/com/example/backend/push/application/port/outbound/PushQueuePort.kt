package com.example.backend.push.application.port.outbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 푸시 발송 요청을 큐에 발행한다. */
interface PushQueuePort {
    fun enqueue(message: PushMessage)
}
