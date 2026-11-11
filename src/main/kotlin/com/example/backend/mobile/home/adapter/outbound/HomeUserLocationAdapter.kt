package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeUserLocationPort
import com.example.backend.user.application.port.inbound.UserLastLocationUseCase
import org.springframework.stereotype.Component

/** BFF 아웃바운드 어댑터. 홈 진입 좌표를 user 도메인 인바운드 포트([UserLastLocationUseCase])로 넘긴다. */
@Component
class HomeUserLocationAdapter(
    private val userLastLocationUseCase: UserLastLocationUseCase,
) : HomeUserLocationPort {
    override fun record(
        userId: Long,
        location: Coordinate,
    ) = userLastLocationUseCase.record(userId, location)
}
