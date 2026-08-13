package com.example.backend.direction.application.port.outbound

import com.example.backend.common.geo.Coordinate

/**
 * 두 지점 사이 보행자 경로 조회 결과.
 * - [Reachable] 도보 이동 가능(소요 초).
 * - [Unreachable] 도보로 갈 수 없는 구간(Tmap NoServiceArea) — 프론트의 "걸어갈 수 없는 거리"(-1) 근거.
 * - [Unknown] 산출 불가(일시적 오류·타임아웃·appKey 미설정·빈 응답) — "모름"이며 도보 불가와는 구분한다.
 */
sealed interface PedestrianRoute {
    data class Reachable(
        val seconds: Int,
    ) : PedestrianRoute

    data object Unreachable : PedestrianRoute

    data object Unknown : PedestrianRoute
}

/** 보행자 경로 아웃바운드 포트. 도보 불가(서비스 불가 구간)와 산출 불가(일시적 오류)를 구분해 돌려준다. */
interface PedestrianRoutePort {
    fun walkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): PedestrianRoute
}
