package com.example.backend.course.adapter.outbound.direction

import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.outbound.WalkingGuide
import com.example.backend.course.application.port.outbound.WalkingRoute
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import com.example.backend.direction.application.port.inbound.WalkingRouteUseCase
import org.springframework.stereotype.Component

/** ACL 어댑터 — course 의 [WalkingRoutePort] 를 direction 의 인바운드 포트로 위임하고 course 소유 DTO로 변환한다. */
@Component
class WalkingRouteAdapter(
    private val walkingRouteUseCase: WalkingRouteUseCase,
) : WalkingRoutePort {
    override fun findWalkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): WalkingRoute? =
        walkingRouteUseCase.walkingRoute(from, to)?.let { route ->
            WalkingRoute(
                distanceMeters = route.distanceMeters,
                seconds = route.seconds,
                path = route.path,
                guides = route.guides.map { WalkingGuide(it.coordinate, it.description) },
            )
        }
}
