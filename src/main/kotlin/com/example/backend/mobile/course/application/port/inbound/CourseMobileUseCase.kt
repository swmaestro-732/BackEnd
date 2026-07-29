package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult

/**
 * 인바운드 포트 — 코스 상세 화면 조합 (BFF).
 * 코스·작성자·장소 도메인의 inbound 포트를 조합해 한 화면 계약을 만든다.
 * 코스 존재/공개범위 판정은 코스 도메인이 담당한다(없음·비공개는 404 COURSE_NOT_FOUND).
 */
interface CourseMobileUseCase {
    /** viewerId 는 로그인 사용자(비로그인이면 null) — 조회자 상태·팔로우 관계·비공개 접근 판정용. */
    fun getScreen(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailScreenResult
}
