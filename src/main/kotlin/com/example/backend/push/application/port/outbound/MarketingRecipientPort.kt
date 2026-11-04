package com.example.backend.push.application.port.outbound

interface MarketingRecipientPort {
    /** 광고성 수신에 동의한 기기의 토큰. [night] 이면 야간 수신까지 동의한 기기만 돌려준다. */
    fun findTokens(night: Boolean): List<String>
}
