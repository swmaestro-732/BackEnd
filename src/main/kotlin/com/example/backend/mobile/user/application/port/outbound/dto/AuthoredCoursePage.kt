package com.example.backend.mobile.user.application.port.outbound.dto

import java.time.Instant

/** BFF 내부 작성 코스 목록 커서. 외부 문자열 커서는 course 위임 어댑터에서 이 타입으로 변환한다. */
data class AuthoredCourseCursor(
    val createdAt: Instant,
    val id: Long,
)

/** BFF 아웃바운드 포트가 반환하는 작성 코스 한 페이지. */
data class AuthoredCoursePage(
    val courses: List<AuthoredCourse>,
    val hasNext: Boolean,
)

/** course 도메인의 공개범위별 코스 개수를 BFF 경계 안으로 복사한 DTO. */
data class CourseCounts(
    val publicCount: Int,
    val followerCount: Int,
    val privateCount: Int,
)
