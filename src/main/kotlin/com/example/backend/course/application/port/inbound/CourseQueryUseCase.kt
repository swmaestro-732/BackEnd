package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseSummary

/**
 * 인바운드 포트 — 코스 조회 유스케이스(목록). 상세·쓰기는 [CourseUseCase] 가 담당한다.
 *
 * - [listByAuthor] 작성자의 발행 코스 요약 목록. viewerId 기준 공개범위(isViewable) 통과분만 내려준다
 *   (작성자 본인 조회면 viewerId==authorId 라 전체 발행분). 비로그인이면 viewerId=null.
 */
interface CourseQueryUseCase {
    fun listByAuthor(
        authorId: Long,
        viewerId: Long?,
    ): List<CourseSummary>
}
