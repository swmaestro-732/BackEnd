package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 유스케이스 출력 — 코스 요약. 작성자 코스 목록·피드·검색 등 여러 화면에서 재사용한다.
 * 카테고리는 도메인 enum 대신 이름 문자열(theme)로 내보낸다(크로스 도메인·BFF 격리, [CourseDetailResult] 와 동일).
 * area 는 검색 결과에서만 채워지는 행정구역 이름으로, 목록/피드 조회에서는 null 이다.
 */
data class CourseSummary(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val area: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
