package com.example.backend.report.adapter.inbound.web.response

import com.example.backend.report.domain.model.Report

data class ReportIdResponse(
    val reportId: Long,
) {
    companion object {
        fun from(report: Report) = ReportIdResponse(reportId = report.id)
    }
}
