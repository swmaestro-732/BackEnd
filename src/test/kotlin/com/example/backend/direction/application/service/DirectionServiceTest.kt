package com.example.backend.direction.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.ErrorCode
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DirectionServiceTest {
    private val a = Coordinate(latitude = 37.5445, longitude = 127.0578)
    private val b = Coordinate(latitude = 37.5432, longitude = 127.0561)
    private val c = Coordinate(latitude = 37.5451, longitude = 127.0549)

    private class FakePort(
        val routeByPair: Map<Pair<Coordinate, Coordinate>, PedestrianRoute>,
    ) : PedestrianRoutePort {
        override fun walkingRoute(
            from: Coordinate,
            to: Coordinate,
        ): PedestrianRoute = routeByPair[from to to] ?: PedestrianRoute.Unknown
    }

    private fun reachable(seconds: Int) = PedestrianRoute.Reachable(seconds)

    @Test
    fun `초를 분으로 올림 변환한다`() {
        val service = DirectionService(FakePort(mapOf((a to b) to reachable(90))))

        assertEquals(2, service.walkingMinutes(a, b))
    }

    @Test
    fun `산출 불가면 null 을 반환한다`() {
        val service = DirectionService(FakePort(mapOf((a to b) to PedestrianRoute.Unknown)))

        assertEquals(null, service.walkingMinutes(a, b))
    }

    @Test
    fun `서비스 불가 구간(Unreachable)은 -1 로 내린다`() {
        val service =
            DirectionService(
                FakePort(
                    mapOf(
                        (a to b) to reachable(300),
                        (b to c) to PedestrianRoute.Unreachable,
                    ),
                ),
            )

        // Unreachable(NoServiceArea) = 걸어갈 수 없는 거리 → -1.
        assertEquals(listOf(5, -1), service.walkingSegments(listOf(a, b, c)))
    }

    @Test
    fun `도보 1시간 초과 구간은 -1 로 내린다`() {
        val service =
            DirectionService(
                FakePort(
                    mapOf(
                        (a to b) to reachable(600), // 10분
                        (b to c) to reachable(3601), // 60분 초과 → 도보 불가
                    ),
                ),
            )

        assertEquals(listOf(10, -1), service.walkingSegments(listOf(a, b, c)))
    }

    @Test
    fun `일시적 오류(Unknown) 구간이 하나라도 있으면 DIRECTION_UNAVAILABLE 로 실패한다`() {
        val service =
            DirectionService(
                FakePort(
                    mapOf(
                        (a to b) to reachable(600),
                        (b to c) to PedestrianRoute.Unknown,
                    ),
                ),
            )

        // Unknown = 산출 불가 → -1(불가)로 속이지 않고 에러(503)로 실패한다.
        val exception = assertThrows<BusinessException> { service.walkingSegments(listOf(a, b, c)) }
        assertEquals(ErrorCode.DIRECTION_UNAVAILABLE, exception.errorCode)
    }
}
