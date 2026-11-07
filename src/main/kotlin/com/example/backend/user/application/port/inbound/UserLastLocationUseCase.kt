package com.example.backend.user.application.port.inbound

import com.example.backend.common.geo.Coordinate

interface UserLastLocationUseCase {
    /** 활성 사용자의 최근 좌표를 덮어쓴다. 탈퇴했거나 없는 사용자면 아무것도 하지 않는다. */
    fun record(
        userId: Long,
        location: Coordinate,
    )
}
