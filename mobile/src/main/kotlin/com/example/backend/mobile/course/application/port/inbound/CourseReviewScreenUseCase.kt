package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenQuery
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenResult

/** 인바운드 포트 — 코스 후기 전체보기 화면 조합(BFF). */
interface CourseReviewScreenUseCase {
    fun getScreen(query: CourseReviewScreenQuery): CourseReviewScreenResult
}
