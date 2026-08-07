package com.example.backend.mobile.user.application.port.outbound

import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCoursePage

/**
 * BFF 아웃바운드 포트 — 작성자 발행 코스 조회. 지금은 course 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 course 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface MyPageCoursePort {
    /** 작성자의 발행 코스 요약 한 페이지. viewerId 기준 공개범위 통과분만. 비로그인이면 viewerId=null. */
    fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
        cursor: String?,
        size: Int,
    ): AuthoredCoursePage
}
