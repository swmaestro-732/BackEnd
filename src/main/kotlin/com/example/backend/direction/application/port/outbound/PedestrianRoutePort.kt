package com.example.backend.direction.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** 보행자 경로 아웃바운드 포트. 두 지점 도보 소요(초), 산출 불가 시 null. */
interface PedestrianRoutePort {
    fun walkingSeconds(
        from: Coordinate,
        to: Coordinate,
    ): Int?
}
