package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** BFF 아웃바운드 포트. 홈 진입 좌표를 사용자 최근 위치로 남긴다(user 도메인 소유). */
interface HomeUserLocationPort {
    fun record(
        userId: Long,
        location: Coordinate,
    )
}
