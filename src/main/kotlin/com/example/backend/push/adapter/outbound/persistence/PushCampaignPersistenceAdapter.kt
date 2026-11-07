package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.adapter.outbound.persistence.exposed.PushCampaignTable
import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.time.toKotlinInstant

@Component
class PushCampaignPersistenceAdapter : PushCampaignPersistencePort {
    // 서비스가 트랜잭션 없이 부르므로 저장은 여기서 바로 커밋한다.
    // pg_advisory_xact_lock 은 트랜잭션이 끝나면 풀린다. 같은 어드민의 동시 요청은 여기서 줄을 서서 앞 요청의 저장을 보고 판단한다.
    @Transactional
    override fun saveUnlessRecent(
        campaign: PushCampaign,
        since: Instant,
    ): Long? {
        TransactionManager.current().exec(
            "SELECT pg_advisory_xact_lock(hashtext(?))",
            listOf(TextColumnType() to "push_campaign:${campaign.adminEmail}"),
        )
        val duplicated =
            !PushCampaignTable
                .selectAll()
                .where {
                    (PushCampaignTable.adminEmail eq campaign.adminEmail) and
                        (PushCampaignTable.title eq campaign.title) and
                        (PushCampaignTable.body eq campaign.body) and
                        (PushCampaignTable.createdAt greaterEq since.toKotlinInstant())
                }.empty()
        if (duplicated) return null
        return PushCampaignTable
            .insertAndGetId {
                it[adminEmail] = campaign.adminEmail
                it[title] = campaign.title
                it[body] = campaign.body
                it[link] = campaign.link
                it[night] = campaign.night
                it[recipientCount] = campaign.recipientCount
            }.value
    }

    @Transactional
    override fun updateCounts(
        id: Long,
        recipientCount: Int,
        enqueuedCount: Int,
    ) {
        PushCampaignTable.update({ PushCampaignTable.id eq id }) {
            it[PushCampaignTable.recipientCount] = recipientCount
            it[PushCampaignTable.enqueuedCount] = enqueuedCount
        }
    }
}
