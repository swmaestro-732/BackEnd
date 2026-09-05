package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult

/**
 * 인바운드 포트 — 코스 상세 화면 조합 (BFF).
 */
interface CourseMobileUseCase {
    fun getScreen(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailScreenResult
}
