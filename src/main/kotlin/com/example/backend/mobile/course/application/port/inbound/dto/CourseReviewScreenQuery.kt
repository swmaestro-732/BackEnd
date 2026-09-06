package com.example.backend.mobile.course.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey

/** 인바운드 포트 입력 DTO — 코스 후기 전체보기 화면 조합. */
data class CourseReviewScreenQuery(
    val courseId: Long,
    val sort: CourseReviewSortKey = CourseReviewSortKey.LATEST,
    val descending: Boolean = true,
    val cursor: String? = null,
    val size: Int = 10,
)
