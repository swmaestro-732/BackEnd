package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.mobile.home.application.port.outbound.HomeProfilePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 홈 헤더 프로필을 user 도메인 인바운드 포트([UserSummaryUseCase])로 조회해 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 user 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class HomeProfileAdapter(
    private val userSummaryUseCase: UserSummaryUseCase,
) : HomeProfilePort {
    override fun findProfile(userId: Long): HomeProfile? =
        userSummaryUseCase
            .findSummaries(listOf(userId))
            .firstOrNull()
            ?.let { HomeProfile(nickname = it.nickname, profileImageUrl = it.profileImageUrl) }
}
