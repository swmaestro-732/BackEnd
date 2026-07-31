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

    override fun walkingSegments(points: List<Coordinate>): List<Int?> =
        points.zipWithNext { from, to -> walkingMinutes(from, to) }
}
