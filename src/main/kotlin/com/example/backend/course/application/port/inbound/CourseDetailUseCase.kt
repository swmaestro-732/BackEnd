package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult

/**
 * 인바운드 포트 — 코스 상세 조회(공개 API).
 * viewerId 는 로그인 사용자 식별자(비로그인이면 null) — 조회자 상태·비공개 접근 판정에 쓰인다.
 */
interface CourseDetailUseCase {
    fun getDetail(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailResult
}
