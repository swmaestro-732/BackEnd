package com.example.backend.push.application.port.outbound

import java.time.Instant

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

    /** 큐에 실제로 넣은 토큰 수를 기록한다. */
    fun updateEnqueuedCount(
        id: Long,
        enqueuedCount: Int,
    )

    /** 같은 어드민이 [since] 이후에 같은 제목과 본문으로 남긴 발송 기록이 있는지. */
    fun existsRecent(
        adminEmail: String,
        title: String,
        body: String,
        since: Instant,
    ): Boolean
}
