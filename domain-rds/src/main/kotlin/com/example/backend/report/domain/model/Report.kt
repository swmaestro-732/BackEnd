package com.example.backend.report.domain.model

import java.time.Instant

data class Report(
    val id: Long,
    val reporterId: Long,
    val targetType: ReportTargetType,
    val targetId: Long,
    val reason: ReportReason,
    val description: String?,
    val status: ReportStatus,
    val createdAt: Instant,
) {
    companion object {
        const val MAX_DESCRIPTION_LENGTH = 500
    }
}
