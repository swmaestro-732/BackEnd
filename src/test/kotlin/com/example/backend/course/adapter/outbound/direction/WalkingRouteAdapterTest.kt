package com.example.backend.course.adapter.outbound.direction

import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.outbound.WalkingGuide
import com.example.backend.direction.application.port.inbound.WalkingRouteUseCase
import com.example.backend.direction.application.port.inbound.dto.WalkingGuideResult
import com.example.backend.direction.application.port.inbound.dto.WalkingRouteResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * [WalkingRouteAdapter] 단위 테스트 — direction 인바운드 포트 결과를 course 소유 DTO 로 옮기고
 * null 은 그대로 전달한다.
 */
class WalkingRouteAdapterTest {
    private val walkingRouteUseCase = mock(WalkingRouteUseCase::class.java)
    private val adapter = WalkingRouteAdapter(walkingRouteUseCase)

    private val from = Coordinate(latitude = 37.5446, longitude = 127.0559)
    private val to = Coordinate(latitude = 37.5420, longitude = 127.0563)

    @Test
    fun `direction 결과를 course DTO 로 변환한다`() {
        `when`(walkingRouteUseCase.walkingRoute(from, to)).thenReturn(
            WalkingRouteResult(
                distanceMeters = 420,
                seconds = 360,
                path = listOf(from, to),
                guides = listOf(WalkingGuideResult(from, "출발")),
            ),
        )

        val route = adapter.findWalkingRoute(from, to)

        assertEquals(420, route?.distanceMeters)
        assertEquals(360, route?.seconds)
        assertEquals(listOf(from, to), route?.path)
        assertEquals(listOf(WalkingGuide(from, "출발")), route?.guides)
    }

    @Test
    fun `도보 불가(null)는 그대로 돌려준다`() {
        `when`(walkingRouteUseCase.walkingRoute(from, to)).thenReturn(null)

        assertNull(adapter.findWalkingRoute(from, to))
    }
}
