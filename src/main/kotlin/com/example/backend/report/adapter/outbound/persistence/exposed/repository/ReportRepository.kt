package com.example.backend.report.adapter.outbound.persistence.exposed.repository

import com.example.backend.report.adapter.outbound.persistence.exposed.ReportTable
import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportStatus
import com.example.backend.report.domain.model.ReportTargetType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

@Repository
class ReportRepository {
    fun insert(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
        reason: ReportReason,
        description: String?,
    ): Report {
        val now = Clock.System.now()
        val id =
            ReportTable
                .insertAndGetId {
                    it[ReportTable.reporterId] = reporterId
                    it[ReportTable.targetType] = targetType
                    it[ReportTable.targetId] = targetId
                    it[ReportTable.reason] = reason
                    it[ReportTable.description] = description
                    it[status] = ReportStatus.PENDING
                    it[createdAt] = now
                    it[updatedAt] = now
                }.value
        return Report(
            id = id,
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            reason = reason,
            description = description,
            status = ReportStatus.PENDING,
            createdAt = now.toJavaInstant(),
        )
    }

    fun existsByReporterAndTarget(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean =
        !ReportTable
            .selectAll()
            .where {
                (ReportTable.reporterId eq reporterId) and
                    (ReportTable.targetType eq targetType) and
                    (ReportTable.targetId eq targetId)
            }.limit(1)
            .empty()
}
