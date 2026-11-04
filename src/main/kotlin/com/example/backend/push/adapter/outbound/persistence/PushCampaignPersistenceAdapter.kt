package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.adapter.outbound.persistence.exposed.PushCampaignTable
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.springframework.stereotype.Component

@Component
class PushCampaignPersistenceAdapter : PushCampaignPersistencePort {
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
