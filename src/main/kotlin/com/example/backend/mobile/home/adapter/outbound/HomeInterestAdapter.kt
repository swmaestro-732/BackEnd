package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.mobile.home.application.port.outbound.HomeInterestPort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeInterest
import com.example.backend.user.application.port.inbound.UserInterestUseCase
import org.springframework.stereotype.Component

/** BFF 아웃바운드 어댑터 — 관심 테마·지역을 user 도메인 인바운드 포트([UserInterestUseCase])로 조회해 BFF 격리 DTO 로 매핑한다. */
@Component
class HomeInterestAdapter(
    private val userInterestUseCase: UserInterestUseCase,
) : HomeInterestPort {
    override fun findInterests(userId: Long): HomeInterest =
        userInterestUseCase
            .findInterests(
                userId,
            ).let { HomeInterest(likeThemes = it.likeThemes, areaCodes = it.areaCodes) }
}
