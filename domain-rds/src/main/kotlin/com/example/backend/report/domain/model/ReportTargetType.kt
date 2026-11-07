package com.example.backend.report.domain.model

/** 신고 대상 종류. enum 이름이 DB 저장 계약이다. */
enum class ReportTargetType {
    COURSE,
    USER,
    COURSE_COMMENT,
}
