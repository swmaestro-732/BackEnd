package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.application.port.outbound.WalkingGuide
import com.example.backend.course.application.port.outbound.WalkingRoute
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

/**
 * [CourseTraceService.findRoute] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * 검증 대상: 출발·도착 장소 검증(같으면 400, 없으면 404), 조회 순서 보정, 도보 경로 → 결과 DTO 변환,
 * 도보 불가(null) 전파.
 */
class CourseTraceServiceTest {
    private val placeLookupPort = mock(PlaceLookupPort::class.java)
    private val walkingRoutePort = mock(WalkingRoutePort::class.java)
    private val service =
        CourseTraceService(
            courseTracePersistencePort = mock(CourseTracePersistencePort::class.java),
            coursePersistencePort = mock(CoursePersistencePort::class.java),
            courseViewPolicy = mock(CourseViewPolicy::class.java),
            placeLookupPort = placeLookupPort,
            walkingRoutePort = walkingRoutePort,
        )

    private val fromPlace = placeRef(22L, "어니언 성수", Coordinate(latitude = 37.5446, longitude = 127.0559))
    private val toPlace = placeRef(23L, "대림창고", Coordinate(latitude = 37.5420, longitude = 127.0563))

    @Test
    fun `출발·도착 장소와 도보 경로를 결과 DTO 로 변환한다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(22L, 23L))).thenReturn(listOf(fromPlace, toPlace))
        val guide = WalkingGuide(fromPlace.coordinate, "출발")
        val route =
            WalkingRoute(
                distanceMeters = 420,
                seconds = 360,
                path = listOf(fromPlace.coordinate, toPlace.coordinate),
                guides = listOf(guide),
            )
        `when`(walkingRoutePort.findWalkingRoute(fromPlace.coordinate, toPlace.coordinate)).thenReturn(route)

        val result = service.findRoute(22L, 23L)

        assertEquals(22L, result.from.placeId)
        assertEquals("어니언 성수", result.from.name)
        assertEquals(fromPlace.coordinate, result.from.coordinate)
        assertEquals(23L, result.to.placeId)
        assertEquals(420, result.route?.distanceMeters)
        assertEquals(360, result.route?.seconds)
        assertEquals(listOf(fromPlace.coordinate, toPlace.coordinate), result.route?.path)
        val guides = result.route?.guides?.map { it.coordinate to it.description }
        assertEquals(listOf(fromPlace.coordinate to "출발"), guides)
    }

    @Test
    fun `조회 결과 순서가 뒤집혀도 요청한 출발·도착 순서로 맞춘다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(23L, 22L))).thenReturn(listOf(fromPlace, toPlace))
        `when`(walkingRoutePort.findWalkingRoute(toPlace.coordinate, fromPlace.coordinate)).thenReturn(null)

        val result = service.findRoute(23L, 22L)

        assertEquals(23L, result.from.placeId)
        assertEquals(22L, result.to.placeId)
    }

    @Test
    fun `도보로 갈 수 없는 구간이면 route 가 null 이다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(22L, 23L))).thenReturn(listOf(fromPlace, toPlace))
        `when`(walkingRoutePort.findWalkingRoute(fromPlace.coordinate, toPlace.coordinate)).thenReturn(null)

        assertNull(service.findRoute(22L, 23L).route)
    }

    @Test
    fun `출발·도착 장소가 같으면 INVALID_INPUT 이고 조회하지 않는다`() {
        val exception = assertThrows<BusinessException> { service.findRoute(22L, 22L) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        verifyNoInteractions(placeLookupPort, walkingRoutePort)
    }

    @Test
    fun `장소가 하나라도 없으면 PLACE_NOT_FOUND 이고 경로를 조회하지 않는다`() {
        `when`(placeLookupPort.findPlacesByIds(listOf(22L, 99L))).thenReturn(listOf(fromPlace))

        val exception = assertThrows<BusinessException> { service.findRoute(22L, 99L) }

        assertEquals(PlaceErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(walkingRoutePort)
    }

    private fun placeRef(
        id: Long,
        name: String,
        coordinate: Coordinate,
    ) = PlaceRef(id = id, category = "CAFE", areaCode = null, name = name, coordinate = coordinate)
}
