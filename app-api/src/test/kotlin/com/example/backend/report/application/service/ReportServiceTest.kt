package com.example.backend.report.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.ReportErrorCode
import com.example.backend.report.application.port.inbound.dto.CreateReportCommand
import com.example.backend.report.application.port.outbound.ReportPersistencePort
import com.example.backend.report.application.port.outbound.ReportTargetLookupPort
import com.example.backend.report.domain.model.Report
import com.example.backend.report.domain.model.ReportReason
import com.example.backend.report.domain.model.ReportStatus
import com.example.backend.report.domain.model.ReportTargetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class ReportServiceTest {
    private val targets = mock(ReportTargetLookupPort::class.java)
    private val reports = mock(ReportPersistencePort::class.java)
    private val service = ReportService(targets, reports)

    @Test
    fun `대상 확인과 중복 검사를 거쳐 저장한 신고를 반환한다`() {
        val saved =
            Report(1L, REPORTER_ID, ReportTargetType.COURSE, 10L, ReportReason.SPAM, "광고", ReportStatus.PENDING, NOW)
        `when`(targets.getOwnerId(ReportTargetType.COURSE, 10L, REPORTER_ID)).thenReturn(OWNER_ID)
        `when`(reports.existsByReporterAndTarget(REPORTER_ID, ReportTargetType.COURSE, 10L)).thenReturn(false)
        `when`(reports.save(REPORTER_ID, ReportTargetType.COURSE, 10L, ReportReason.SPAM, "광고")).thenReturn(saved)

        val result = service.create(command(ReportTargetType.COURSE, 10L, "광고"))

        assertEquals(saved, result)
        val order = inOrder(targets, reports)
        order.verify(targets).getOwnerId(ReportTargetType.COURSE, 10L, REPORTER_ID)
        order.verify(reports).existsByReporterAndTarget(REPORTER_ID, ReportTargetType.COURSE, 10L)
        order.verify(reports).save(REPORTER_ID, ReportTargetType.COURSE, 10L, ReportReason.SPAM, "광고")
    }

    @Test
    fun `본인 콘텐츠면 CANNOT_REPORT_SELF 이고 저장소를 건드리지 않는다`() {
        `when`(targets.getOwnerId(ReportTargetType.USER, REPORTER_ID, REPORTER_ID)).thenReturn(REPORTER_ID)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(command(ReportTargetType.USER, REPORTER_ID, null))
            }

        assertEquals(ReportErrorCode.CANNOT_REPORT_SELF, exception.errorCode)
        verifyNoInteractions(reports)
    }

    @Test
    fun `이미 신고한 대상이면 REPORT_ALREADY_EXISTS 이고 저장하지 않는다`() {
        `when`(targets.getOwnerId(ReportTargetType.COURSE_COMMENT, 5L, REPORTER_ID)).thenReturn(OWNER_ID)
        `when`(reports.existsByReporterAndTarget(REPORTER_ID, ReportTargetType.COURSE_COMMENT, 5L)).thenReturn(true)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(command(ReportTargetType.COURSE_COMMENT, 5L, null))
            }

        assertEquals(ReportErrorCode.REPORT_ALREADY_EXISTS, exception.errorCode)
        verify(reports, never()).save(REPORTER_ID, ReportTargetType.COURSE_COMMENT, 5L, ReportReason.SPAM, null)
    }

    @Test
    fun `대상 조회 예외는 그대로 전파하고 저장소를 건드리지 않는다`() {
        `when`(targets.getOwnerId(ReportTargetType.COURSE, 10L, REPORTER_ID))
            .thenThrow(BusinessException(CourseErrorCode.COURSE_NOT_FOUND))

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(command(ReportTargetType.COURSE, 10L, null))
            }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(reports)
    }

    private fun command(
        type: ReportTargetType,
        targetId: Long,
        description: String?,
    ) = CreateReportCommand(REPORTER_ID, type, targetId, ReportReason.SPAM, description)

    private companion object {
        const val REPORTER_ID = 1L
        const val OWNER_ID = 2L
        val NOW: Instant = Instant.parse("2026-10-10T00:00:00Z")
    }
}
