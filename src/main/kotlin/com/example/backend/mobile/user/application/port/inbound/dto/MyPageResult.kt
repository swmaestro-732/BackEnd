package com.example.backend.mobile.user.application.port.inbound.dto

import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCourse
import com.example.backend.mobile.user.application.port.outbound.dto.ProfileSnapshot

/**
 * 마이페이지 화면 조합 결과 (BFF) — 프로필 + 그 사용자의 공개 코스 목록.
 * 조합은 BFF 자신의 아웃바운드 포트로만 이뤄지며, 재료는 도메인 타입이 아닌 BFF 격리 DTO 다
 * (MSA 분리 대비 — [ProfileSnapshot]·[AuthoredCourse]).
 */
data class MyPageResult(
    val profile: ProfileSnapshot,
    val courses: List<AuthoredCourse>,
)
