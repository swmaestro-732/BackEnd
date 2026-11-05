package com.example.backend.push.application.port.inbound.dto

/** 마케팅 푸시 발송 요청. 제목과 본문은 어드민이 입력한 그대로이고 서비스 표기와 광고 표기는 서비스가 붙인다. */
data class SendMarketingPushCommand(
    val adminEmail: String,
    val title: String,
    val body: String,
    val link: String,
)

/** [recipientCount] 는 대상 기기 수, [enqueuedCount] 는 그중 큐에 실제로 넣은 수다. */
data class MarketingPushResult(
    val campaignId: Long,
    val recipientCount: Int,
    val enqueuedCount: Int,
)
