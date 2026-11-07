package com.example.backend.mobile.user.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 작성자의 발행 코스 요약. course 도메인 응답을 BFF 안으로 복사한 격리 DTO다.
 * themes 는 도메인 enum 이 아니라 이름 문자열 목록(크로스 도메인·BFF 격리).
 * ([com.example.backend.mobile.user.application.port.outbound.MyPageCoursePort]).
 */
data class AuthoredCourse(
    val id: Long,
    val title: String,
    val coverImageUrl: String?,
    val themes: List<String>,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
