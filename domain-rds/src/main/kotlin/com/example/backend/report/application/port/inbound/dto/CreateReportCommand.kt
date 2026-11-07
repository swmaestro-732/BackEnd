package com.example.backend.report.application.port.inbound.dto

import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportTargetType

data class CreateReportCommand(
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val description: String?,
)
