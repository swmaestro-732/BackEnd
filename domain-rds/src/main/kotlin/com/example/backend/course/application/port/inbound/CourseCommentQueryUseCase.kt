package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseCommentPage
import com.example.backend.course.application.port.inbound.dto.CourseCommentRef

interface CourseCommentQueryUseCase {
    fun list(
        courseId: Long,
        viewerId: Long?,
        cursor: Long?,
        size: Int,
    ): CourseCommentPage

    /** 미삭제 댓글의 소속 코스·작성자. 없으면 COURSE_COMMENT_NOT_FOUND. 다른 도메인(신고 등)의 대상 확인용 — 부모 코스 열람 검사는 호출부가 courseId 로 한다. */
    fun getRef(commentId: Long): CourseCommentRef
}
