package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.adapter.outbound.persistence.exposed.PushCampaignTable
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PushCampaignPersistenceAdapter : PushCampaignPersistencePort {
    // 서비스가 트랜잭션 없이 부르므로 저장은 여기서 바로 커밋한다.
    @Transactional
    override fun save(campaign: PushCampaign): Long =
        PushCampaignTable
            .insertAndGetId {
                it[adminEmail] = campaign.adminEmail
                it[title] = campaign.title
                it[body] = campaign.body
                it[link] = campaign.link
                it[night] = campaign.night
                it[recipientCount] = campaign.recipientCount
            }.value
}
