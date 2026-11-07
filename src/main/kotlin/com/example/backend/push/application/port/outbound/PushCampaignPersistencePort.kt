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
    /**
     * 같은 어드민이 [since] 이후 같은 제목과 본문으로 남긴 기록이 없을 때만 [campaign] 을 저장하고 id 를 돌려준다. 있으면 null.
     * 어드민별 잠금 아래에서 확인과 저장을 한 번에 해, 버튼 연타처럼 동시에 들어온 요청도 하나만 저장된다.
     */
    fun saveUnlessRecent(
        campaign: PushCampaign,
        since: Instant,
    ): Long?

    /** 실제 대상 수와 큐에 넣은 토큰 수를 기록한다. */
    fun updateCounts(
        id: Long,
        recipientCount: Int,
        enqueuedCount: Int,
    )

    /** 아무것도 발행하기 전에 실패한 기록을 지워 같은 문구로 바로 다시 보낼 수 있게 한다. */
    fun delete(id: Long)
}
