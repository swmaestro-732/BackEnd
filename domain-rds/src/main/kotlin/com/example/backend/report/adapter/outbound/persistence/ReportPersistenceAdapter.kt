package com.example.backend.report.adapter.outbound.persistence

import com.example.backend.report.adapter.outbound.persistence.exposed.repository.ReportRepository
import com.example.backend.report.application.port.outbound.ReportPersistencePort
import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportTargetType
import org.springframework.stereotype.Component

@Component
class ReportPersistenceAdapter(
    private val reportRepository: ReportRepository,
) : ReportPersistencePort {
    override fun save(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
        reason: ReportReason,
        description: String?,
    ): Report = reportRepository.insert(reporterId, targetType, targetId, reason, description)

    override fun existsByReporterAndTarget(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean = reportRepository.existsByReporterAndTarget(reporterId, targetType, targetId)
}
