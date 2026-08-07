package com.example.backend.direction.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.inbound.WalkingDurationUseCase
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
    ): Int? = pedestrianRoutePort.walkingSeconds(from, to)?.let { ceil(it / 60.0).toInt() }

    /**
     * 구간별 도보 소요(분). 산출 불가(Tmap 에러·서비스 불가 구간 → null) 또는 1시간 초과면 [UNWALKABLE](-1) 로 내린다.
     * -1 은 "도보로 갈 수 없는 거리"를 뜻한다. 예: [10, -1, 8].
     */
    override fun walkingSegments(points: List<Coordinate>): List<Int> =
        points.zipWithNext { from, to ->
            walkingMinutes(from, to).let { if (it == null || it > MAX_WALKABLE_MINUTES) UNWALKABLE else it }
        }

    private companion object {
        /** 도보로 갈 수 있다고 보는 상한(분). 1시간 초과는 걸어갈 수 없는 거리로 본다. */
        const val MAX_WALKABLE_MINUTES = 60

        /** 도보 불가(에러·서비스 불가 구간·1시간 초과) 표시값. */
        const val UNWALKABLE = -1
    }
}
