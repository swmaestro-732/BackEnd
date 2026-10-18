package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 유스케이스 출력 — 코스 요약. 작성자 코스 목록·피드·검색 등 여러 화면에서 재사용한다.
 * 카테고리는 도메인 enum 대신 이름 문자열(theme)로 내보낸다(크로스 도메인·BFF 격리, [CourseDetailResult] 와 동일).
 * area 는 코스 지역 이름(미입력이면 null)이다.
 * placeCount, walkingMinutes(구간 도보 분 합, 미입력 구간은 0)는 공개 피드(listPublic)에서만 채워지고 그 외에는 null 이다.
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
    val placeCount: Int?,
    val walkingMinutes: Int?,
)
