package com.example.backend.push.application.port.outbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 푸시 발송 요청을 큐에 발행한다. */
interface PushQueuePort {
    /** 큐에 넣었으면 true. 큐 미설정이나 발행 실패는 예외 대신 false 로 알린다. */
    fun enqueue(message: PushMessage): Boolean
}
