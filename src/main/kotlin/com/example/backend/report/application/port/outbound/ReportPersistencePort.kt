package com.example.backend.report.application.port.outbound

import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportTargetType

interface ReportPersistencePort {
    fun save(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
        reason: ReportReason,
        description: String?,
    ): Report

    fun existsByReporterAndTarget(
        reporterId: Long,
        targetType: ReportTargetType,
        targetId: Long,
    ): Boolean
}
