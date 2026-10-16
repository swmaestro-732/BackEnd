package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.client.tmap.TmapPedestrianClient
import com.example.backend.client.tmap.TmapPedestrianResult
import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import org.springframework.stereotype.Component

/** Tmap 결과 → [PedestrianRoute]. NoServiceArea 만 도보 불가로 확정하고, 실패는 "모름"([PedestrianRoute.Unknown]). */
@Component
class TmapPedestrianAdapter(
    private val tmapPedestrianClient: TmapPedestrianClient,
) : PedestrianRoutePort {
    override fun walkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): PedestrianRoute =
        when (val result = tmapPedestrianClient.route(from, to)) {
            is TmapPedestrianResult.Found -> PedestrianRoute.Reachable(result.totalSeconds)
            TmapPedestrianResult.NoServiceArea -> PedestrianRoute.Unreachable
            TmapPedestrianResult.Failed -> PedestrianRoute.Unknown
        }
}
