package com.example.backend.push.application.port.inbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 큐에서 수신한 메시지를 FCM 으로 발송한다. push 도메인의 SQS 수신 어댑터만 사용하고 다른 도메인은 [PushUseCase] 를 쓴다. */
interface PushDeliveryUseCase {
    fun deliver(message: PushMessage)
}
