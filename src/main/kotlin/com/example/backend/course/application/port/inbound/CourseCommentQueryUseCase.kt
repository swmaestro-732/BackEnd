package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CourseCommentPage

interface CourseCommentQueryUseCase {
    fun list(
        courseId: Long,
        viewerId: Long?,
        cursor: Long?,
        size: Int,
    ): CourseCommentPage

    /** 미삭제 댓글의 작성자 id. 없으면 COURSE_COMMENT_NOT_FOUND. 다른 도메인(신고 등)의 대상 확인용. */
    fun getAuthorId(commentId: Long): Long
}
