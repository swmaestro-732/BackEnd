package com.example.backend.report.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ReportErrorCode
import com.example.backend.report.application.port.inbound.ReportUseCase
import com.example.backend.report.application.port.inbound.dto.CreateReportCommand
import com.example.backend.report.application.port.outbound.ReportPersistencePort
import com.example.backend.report.application.port.outbound.ReportTargetLookupPort
import com.example.backend.report.domain.model.Report
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReportService(
    private val targetLookupPort: ReportTargetLookupPort,
    private val reportPersistencePort: ReportPersistencePort,
) : ReportUseCase {
    override fun create(command: CreateReportCommand): Report {
        val ownerId = targetLookupPort.getOwnerId(command.targetType, command.targetId, command.reporterId)
        if (ownerId == command.reporterId) {
            throw BusinessException(ReportErrorCode.CANNOT_REPORT_SELF)
        }
        if (reportPersistencePort.existsByReporterAndTarget(command.reporterId, command.targetType, command.targetId)) {
            throw BusinessException(ReportErrorCode.REPORT_ALREADY_EXISTS)
        }
        return reportPersistencePort.save(
            reporterId = command.reporterId,
            targetType = command.targetType,
            targetId = command.targetId,
            reason = command.reason,
            description = command.description,
        )
    }
}
