package com.example.backend.course.application.port.outbound

import com.example.backend.common.geo.Coordinate

/**
 * 아웃바운드 포트 — 두 지점 사이 도보 경로(따라가기 화면의 다음 장소 안내).
 * course 코어는 direction 을 직접 알지 않고 이 포트와 course 소유 DTO([WalkingRoute])에만 의존한다.
 * 실제 direction 인바운드 포트 호출은 ACL 어댑터([com.example.backend.course.adapter.outbound.direction.WalkingRouteAdapter])가 담당한다.
 */
interface WalkingRoutePort {
    /** 도보로 갈 수 없는 구간이면 null. 산출 불가(일시적 오류)는 `WALKING_ROUTE_UNAVAILABLE`(503/5033)로 실패한다. */
    fun findWalkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): WalkingRoute?
}

/** course 소유 DTO — 거리(m)·소요 초·지도에 그릴 폴리라인·안내 지점. */
data class WalkingRoute(
    val distanceMeters: Int,
    val seconds: Int,
    val path: List<Coordinate>,
    val guides: List<WalkingGuide>,
)

data class WalkingGuide(
    val coordinate: Coordinate,
    val description: String,
)
