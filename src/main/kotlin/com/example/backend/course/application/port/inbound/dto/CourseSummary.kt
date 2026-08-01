package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 유스케이스 출력 — 코스 요약. 작성자 코스 목록 등 여러 화면에서 재사용한다.
 * 카테고리는 도메인 enum 대신 이름 문자열(theme)로 내보낸다(크로스 도메인·BFF 격리, [CourseDetailResult] 와 동일).
 */
data class CourseSummary(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
