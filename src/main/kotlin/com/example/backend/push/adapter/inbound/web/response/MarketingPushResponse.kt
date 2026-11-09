package com.example.backend.push.adapter.inbound.web.response

import com.example.backend.push.application.port.inbound.dto.MarketingPushResult

/** 마케팅 푸시 발송 응답. */
data class MarketingPushResponse(
    val campaignId: Long,
    val recipientCount: Int,
    val enqueuedCount: Int,
) {
    companion object {
        fun from(result: MarketingPushResult): MarketingPushResponse =
            MarketingPushResponse(
                campaignId = result.campaignId,
                recipientCount = result.recipientCount,
                enqueuedCount = result.enqueuedCount,
            )
    }
}
