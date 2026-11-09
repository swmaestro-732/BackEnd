package com.example.backend.push.application.port.inbound

import com.example.backend.push.application.port.inbound.dto.MarketingPushResult
import com.example.backend.push.application.port.inbound.dto.SendMarketingPushCommand

/** 어드민이 광고성 수신 동의 기기 전체에 마케팅 푸시를 보낸다. */
interface MarketingPushUseCase {
    fun send(command: SendMarketingPushCommand): MarketingPushResult
}
