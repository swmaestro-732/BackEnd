package com.example.backend.report.adapter.inbound.web

import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.report.adapter.inbound.web.request.CreateReportRequest
import com.example.backend.report.adapter.inbound.web.response.ReportIdResponse
import com.example.backend.report.application.port.inbound.ReportUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val reportUseCase: ReportUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AccessTokenRequired
    fun create(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: CreateReportRequest,
    ): ApiResponse<ReportIdResponse> {
        val report = reportUseCase.create(request.toCommand(userId))
        return ApiResponse.success(ReportIdResponse.from(report), "신고가 접수되었습니다.")
    }
}
