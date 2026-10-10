package com.example.backend.report.adapter.outbound.persistence.exposed

import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportStatus
import com.example.backend.report.domain.model.ReportTargetType
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

internal object ReportTable : LongIdTable("reports") {
    val reporterId = long("reporter_id")
    val targetType = enumerationByName<ReportTargetType>("target_type", 32)
    val targetId = long("target_id")
    val reason = enumerationByName<ReportReason>("reason", 32)
    val description = varchar("description", Report.MAX_DESCRIPTION_LENGTH).nullable()
    val status = enumerationByName<ReportStatus>("status", 32)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}
