package com.example.backend.direction.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.inbound.dto.WalkingRouteResult

/** 보행자 경로 인바운드 포트(공개 API). 다른 도메인(course)이 도보 경로가 필요하면 이 포트로만 접근한다. */
interface WalkingRouteUseCase {
    /**
     * 두 지점 사이 도보 경로. 도보로 갈 수 없는 구간(Tmap NoServiceArea)이면 null.
     * 산출 불가(일시적 오류·타임아웃)는 null 로 속이지 않고 `WALKING_ROUTE_UNAVAILABLE`(503/5033)로 실패한다.
     */
    fun walkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): WalkingRouteResult?
}
