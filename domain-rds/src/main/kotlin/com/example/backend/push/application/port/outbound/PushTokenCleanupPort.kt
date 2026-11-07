package com.example.backend.push.application.port.outbound

interface PushTokenCleanupPort {
    /** 발송 결과 무효로 판정된 토큰을 더는 발송 대상이 되지 않게 정리한다. */
    fun removeInvalid(tokens: Collection<String>)
}
