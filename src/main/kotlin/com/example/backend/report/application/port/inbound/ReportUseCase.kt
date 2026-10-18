package com.example.backend.report.application.port.inbound

import com.example.backend.report.application.port.inbound.dto.CreateReportCommand
import com.example.backend.report.domain.model.Report

interface ReportUseCase {
    /** 대상이 없으면 해당 도메인의 NOT_FOUND, 본인 콘텐츠면 4006, 이미 신고했으면 4099. */
    fun create(command: CreateReportCommand): Report
}
