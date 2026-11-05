package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.adapter.outbound.persistence.exposed.PushCampaignTable
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.time.toKotlinInstant

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

    @Transactional
    override fun updateEnqueuedCount(
        id: Long,
        enqueuedCount: Int,
    ) {
        PushCampaignTable.update({ PushCampaignTable.id eq id }) { it[PushCampaignTable.enqueuedCount] = enqueuedCount }
    }

    @Transactional(readOnly = true)
    override fun existsRecent(
        adminEmail: String,
        title: String,
        body: String,
        since: Instant,
    ): Boolean =
        PushCampaignTable
            .selectAll()
            .where {
                (PushCampaignTable.adminEmail eq adminEmail) and (PushCampaignTable.title eq title) and
                    (PushCampaignTable.body eq body) and (PushCampaignTable.createdAt greaterEq since.toKotlinInstant())
            }.empty()
            .not()
}
