package com.example.backend.direction.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.inbound.WalkingDurationUseCase
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import org.springframework.stereotype.Service
import kotlin.math.ceil

/** 보행자 이동 시간 서비스. 초 → 분(올림) 변환과 구간별 계산을 담당한다. */
@Service
class DirectionService(
    private val pedestrianRoutePort: PedestrianRoutePort,
) : WalkingDurationUseCase {
    override fun walkingMinutes(
        from: Coordinate,
        to: Coordinate,
    ): Int? =
        (pedestrianRoutePort.walkingRoute(from, to) as? PedestrianRoute.Reachable)?.let {
            ceil(it.seconds / 60.0).toInt()
        }

    /**
     * 구간별 도보 소요(분). 반환 크기 = points.size - 1. 세 상태를 구분한다:
     * - **분(양수)**: 도보 이동 가능.
     * - **[UNWALKABLE](-1)**: 도보로 갈 수 없음 — 서비스 불가 구간(Tmap NoServiceArea) 또는 1시간 초과. 프론트가 "걸어갈 수 없는 거리"로 표시.
     * - **null**: 산출 불가(일시적 오류·타임아웃 등) — 도보 불가가 아니라 "모름"이라 -1 과 구분한다.
     * 예: [10, -1, null, 8].
     */
    override fun walkingSegments(points: List<Coordinate>): List<Int?> =
        points.zipWithNext { from, to -> segmentMinutes(from, to) }

    private fun segmentMinutes(
        from: Coordinate,
        to: Coordinate,
    ): Int? =
        when (val route = pedestrianRoutePort.walkingRoute(from, to)) {
            is PedestrianRoute.Reachable -> {
                val minutes = ceil(route.seconds / 60.0).toInt()
                // 도보 이동 가능이라도 1시간 초과는 걸어갈 수 없는 거리로 보고 -1.
                if (minutes > MAX_WALKABLE_MINUTES) UNWALKABLE else minutes
            }

            // 서비스 불가 구간 — 도보로 갈 수 없음.
            PedestrianRoute.Unreachable -> {
                UNWALKABLE
            }

            // 일시적 오류 등 — 알 수 없음(도보 불가와 구분해 null).
            PedestrianRoute.Unknown -> {
                null
            }
        }

    private companion object {
        /** 도보로 갈 수 있다고 보는 상한(분). 1시간 초과는 걸어갈 수 없는 거리로 본다. */
        const val MAX_WALKABLE_MINUTES = 60

        /** 도보 불가(서비스 불가 구간·1시간 초과) 표시값. */
        const val UNWALKABLE = -1
    }
}
