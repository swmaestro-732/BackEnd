package com.example.backend.course.domain.model

import com.example.backend.common.persistence.CodedEnum

/** 코스 리뷰 상태. code 는 DB 저장 계약 — 변경 금지. */
enum class CourseReviewStatus(
    override val code: Short,
) : CodedEnum {
    PUBLISHED(0),
    HIDDEN(1),
    DELETED(2),
}
