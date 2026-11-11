package com.example.backend.user.application.port.inbound

/** 광고성 푸시 수신 대상 기기 조회. push 도메인의 마케팅 발송이 쓴다. */
interface MarketingDeviceUseCase {
    /** 토큰이 있고 광고성 수신에 동의한 기기의 토큰. [night] 이면 야간 수신 동의도 있어야 한다. */
    fun findMarketingTokens(night: Boolean): List<String>
}
