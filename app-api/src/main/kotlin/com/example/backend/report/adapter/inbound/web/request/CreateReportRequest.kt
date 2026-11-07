package com.example.backend.report.adapter.inbound.web.request

import com.example.backend.report.application.port.inbound.dto.CreateReportCommand
import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportTargetType
import jakarta.validation.constraints.Size

data class CreateReportRequest(
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    @field:Size(max = Report.MAX_DESCRIPTION_LENGTH)
    val description: String? = null,
) {
    fun toCommand(reporterId: Long): CreateReportCommand =
        CreateReportCommand(
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            reason = reason,
            description = description?.takeIf { it.isNotBlank() },
        )
}
