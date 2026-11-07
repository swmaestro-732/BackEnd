package com.example.backend.report.domain.model

/** 신고 처리 상태. enum 이름이 DB 저장 계약이다. */
enum class ReportStatus {
    PENDING,
    RESOLVED,
    REJECTED,
}
