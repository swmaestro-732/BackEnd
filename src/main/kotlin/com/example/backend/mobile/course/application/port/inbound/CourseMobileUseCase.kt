package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CoursePreviewCommand
import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult

/**
 * 인바운드 포트 — 코스 상세 화면 조합 (BFF).
 */
interface CourseMobileUseCase {
    fun getScreen(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailScreenResult

    /** 어드민 미리보기: 저장 전 초안을 실제 상세 화면과 같은 결과로 조합한다(쓰기, 외부 호출 없음). */
    fun getPreviewScreen(command: CoursePreviewCommand): CourseDetailScreenResult
}
