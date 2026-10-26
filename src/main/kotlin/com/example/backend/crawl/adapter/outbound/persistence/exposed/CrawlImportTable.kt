package com.example.backend.crawl.adapter.outbound.persistence.exposed

import com.example.backend.crawl.domain.model.CrawlImportStatus
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

internal object CrawlImportTable : LongIdTable("crawl_imports") {
    val shortcode = varchar("shortcode", 64).uniqueIndex()
    val runId = varchar("run_id", 64)
    val status = enumerationByName<CrawlImportStatus>("status", 16)
    val courseId = long("course_id").nullable()
    val reason = text("reason").nullable()
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}
