package com.example.backend.course.application.port.inbound.dto

/** 댓글의 소속 코스와 작성자만 담은 참조 — 다른 도메인(신고 등)의 대상 확인용. */
data class CourseCommentRef(
    val courseId: Long,
    val authorId: Long,
)
