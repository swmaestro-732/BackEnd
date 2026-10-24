package com.example.backend.course.application.port.inbound.dto

/** 활성 코스의 소유자와 제목만 담는 내부 조회 계약. */
data class CourseOwnerSummary(
    val courseId: Long,
    val authorId: Long,
    val title: String,
)
