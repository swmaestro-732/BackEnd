package com.example.backend.push.application.port.inbound.dto

/** 마케팅 푸시 발송 요청. 제목과 본문은 어드민이 입력한 그대로이고 광고 표기는 서비스가 붙인다. */
data class SendMarketingPushCommand(
    val adminEmail: String,
    val title: String,
    val body: String,
    val link: String,
)

data class MarketingPushResult(
    val campaignId: Long,
    val recipientCount: Int,
)
