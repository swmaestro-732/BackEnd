package com.example.backend.mobile.user.application.port.inbound.dto

import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 마이페이지 화면 조합 결과 (BFF) — 프로필(user) + 그 사용자의 공개 코스 목록(course).
 * 조합은 도메인 인바운드 포트만으로 이뤄진다(직접 영속성 접근 없음).
 */
data class MyPageResult(
    val profile: UserProfileResult,
    val courses: List<CourseSummary>,
)
