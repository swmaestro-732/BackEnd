package com.example.backend.push.application.port.outbound

/** 마케팅 푸시 발송 기록. 제목과 본문은 실제로 나간 문구다. */
data class PushCampaign(
    val adminEmail: String,
    val title: String,
    val body: String,
    val link: String,
    val night: Boolean,
    val recipientCount: Int,
)

interface PushCampaignPersistencePort {
    /** 발송 기록을 남기고 id 를 돌려준다. */
    fun save(campaign: PushCampaign): Long
}
