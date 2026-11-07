package com.example.backend.push.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

internal object PushCampaignTable : LongIdTable("push_campaigns") {
    val adminEmail = text("admin_email")
    val title = text("title")
    val body = text("body")
    val link = text("link")
    val night = bool("night")
    val recipientCount = integer("recipient_count")
    val enqueuedCount = integer("enqueued_count").default(0)
    val createdAt = timestamp("created_at").databaseGenerated()
}
