package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/** 작성자 코스 목록의 정렬 키(createdAt DESC, id DESC)를 가리키는 키셋 커서. */
data class AuthorCourseCursor(
    val createdAt: Instant,
    val id: Long,
)
