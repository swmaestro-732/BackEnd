package com.example.backend.course.domain.model

import com.example.backend.common.persistence.CodedEnum

/** 코스 공개 범위. code 는 DB 저장 계약 — 변경 금지. */
enum class CourseVisibility(
    override val code: Short,
) : CodedEnum {
    PUBLIC(0),
    FRIENDS(1),
    PRIVATE(2),
}
