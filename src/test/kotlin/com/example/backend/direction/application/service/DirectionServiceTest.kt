package com.example.backend.direction.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DirectionServiceTest {
    private val a = Coordinate(latitude = 37.5445, longitude = 127.0578)
    private val b = Coordinate(latitude = 37.5432, longitude = 127.0561)
    private val c = Coordinate(latitude = 37.5451, longitude = 127.0549)

    private class FakePort(
        val secondsByPair: Map<Pair<Coordinate, Coordinate>, Int?>,
    ) : PedestrianRoutePort {
        override fun walkingSeconds(
            from: Coordinate,
            to: Coordinate,
        ): Int? = secondsByPair[from to to]
    }

    @Test
    fun `초를 분으로 올림 변환한다`() {
        val service = DirectionService(FakePort(mapOf((a to b) to 90)))

        assertEquals(2, service.walkingMinutes(a, b))
    }

    @Test
    fun `초가 없으면 null 을 반환한다`() {
        val service = DirectionService(FakePort(mapOf((a to b) to null)))

        assertEquals(null, service.walkingMinutes(a, b))
    }

    @Test
    fun `구간별 도보 시간을 계산하고 산출 불가 구간은 -1 로 내린다`() {
        val service =
            DirectionService(
                FakePort(
                    mapOf(
                        (a to b) to 300,
                        (b to c) to null,
                    ),
                ),
            )

        // 산출 불가(null) 구간은 도보 불가라 -1.
        assertEquals(listOf(5, -1), service.walkingSegments(listOf(a, b, c)))
    }

    @Test
    fun `도보 1시간 초과 구간은 -1 로 내린다`() {
        val service =
            DirectionService(
                FakePort(
                    mapOf(
                        (a to b) to 600, // 10분
                        (b to c) to 3601, // 60분 초과 → 도보 불가
                    ),
                ),
            )

        assertEquals(listOf(10, -1), service.walkingSegments(listOf(a, b, c)))
    }
}
