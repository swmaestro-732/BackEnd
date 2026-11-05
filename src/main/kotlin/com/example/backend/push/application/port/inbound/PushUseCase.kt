package com.example.backend.push.application.port.inbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 다른 도메인이 쓰는 푸시 발행 계약. 메시지 검증 실패는 호출 시점에 IllegalArgumentException 으로 드러난다. */
interface PushUseCase {
    /** 실제로 큐에 넣은 토큰 수를 돌려준다. 큐 미설정이나 발행 실패로 빠진 묶음은 세지 않는다. */
    fun request(message: PushMessage): Int
}
