package com.example.backend.report.domain.model

/** 신고 사유. enum 이름이 DB 저장 계약이다. */
enum class ReportReason {
    SPAM,
    ABUSE,
    OBSCENE,
    PRIVACY,
    OTHER,
}
