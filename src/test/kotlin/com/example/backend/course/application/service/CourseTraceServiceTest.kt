package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.application.port.outbound.WalkingGuide
import com.example.backend.course.application.port.outbound.WalkingRoute
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import com.example.backend.course.domain.model.CourseTraceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

/**
 * [CourseTraceService] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * - findRoute: 출발·도착 장소 검증(같으면 400, 없으면 404), 조회 순서 보정, 도보 경로 → 결과 DTO 변환, 도보 불가(null) 전파.
 * - findLatestInProgress: 진행 중 1건 조회.
 * - list: 커서 디코딩 → beforeId 전달, size+1 조회로 hasNext 판정·페이지 절단·nextCursor 생성, 행 → Summary 변환.
 */
class CourseTraceServiceTest {
    private val courseTracePersistencePort = mock(CourseTracePersistencePort::class.java)
    private val placeLookupPort = mock(PlaceLookupPort::class.java)
    private val walkingRoutePort = mock(WalkingRoutePort::class.java)
    private val service =
        CourseTraceService(
            courseTracePersistencePort = courseTracePersistencePort,
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

    // --- list ---

    @Test
    fun `목록 — size 보다 많이 조회되면 잘라내고 hasNext 와 마지막 행 id 커서를 돌려준다`() {
        `when`(courseTracePersistencePort.findByUserAndStatus(1L, CourseTraceStatus.COMPLETED, null, 3))
            .thenReturn(listOf(completedRow(30L), completedRow(20L), completedRow(10L)))

        val result =
            service.list(
                CourseTracesQuery(userId = 1L, status = CourseTraceStatus.COMPLETED, cursor = null, size = 2),
            )

        assertEquals(true, result.hasNext)
        assertEquals(listOf(30L, 20L), result.traces.map { it.id })
        assertEquals(20L, CourseTraceCursorCodec.decode(result.nextCursor))
    }

    @Test
    fun `목록 — 커서를 beforeId 로 풀어 넘기고 마지막 페이지면 nextCursor 가 null 이다`() {
        `when`(courseTracePersistencePort.findByUserAndStatus(1L, CourseTraceStatus.COMPLETED, 20L, 3))
            .thenReturn(listOf(completedRow(10L)))

        val result =
            service.list(
                CourseTracesQuery(
                    userId = 1L,
                    status = CourseTraceStatus.COMPLETED,
                    cursor = CourseTraceCursorCodec.encode(20L),
                    size = 2,
                ),
            )

        assertEquals(false, result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(10L), result.traces.map { it.id })
    }

    @Test
    fun `목록 — 세션 행을 상태·시각·소요 시간·거리 그대로 Summary 로 옮긴다`() {
        val startedAt = Instant.parse("2026-11-02T10:00:00Z")
        val row =
            CourseTraceRow(
                id = 5L,
                userId = 1L,
                courseId = 7L,
                status = CourseTraceStatus.IN_PROGRESS,
                startedAt = startedAt,
                endedAt = null,
                durationMinutes = 0,
                distanceMeters = 0,
            )
        `when`(
            courseTracePersistencePort.findByUserAndStatus(1L, CourseTraceStatus.IN_PROGRESS, null, 11),
        ).thenReturn(listOf(row))

        val summary =
            service
                .list(
                    CourseTracesQuery(userId = 1L, status = CourseTraceStatus.IN_PROGRESS, cursor = null, size = 10),
                ).traces
                .single()

        assertEquals(
            CourseTraceSummary(
                id = 5L,
                courseId = 7L,
                status = CourseTraceStatus.IN_PROGRESS,
                startedAt = startedAt,
                endedAt = null,
                durationMinutes = 0,
                distanceMeters = 0,
            ),
            summary,
        )
    }

    @Test
    fun `최근 진행 중 — 진행 중 세션을 1건만 조회해 돌려주고 없으면 null 이다`() {
        val row = completedRow(5L).copy(status = CourseTraceStatus.IN_PROGRESS, endedAt = null)
        `when`(courseTracePersistencePort.findByUserAndStatus(1L, CourseTraceStatus.IN_PROGRESS, null, 2))
            .thenReturn(listOf(row))
        `when`(courseTracePersistencePort.findByUserAndStatus(2L, CourseTraceStatus.IN_PROGRESS, null, 2))
            .thenReturn(emptyList())

        assertEquals(5L, service.findLatestInProgress(1L)?.id)
        assertNull(service.findLatestInProgress(2L))
    }

    @Test
    fun `목록 — 잘못된 커서는 INVALID_INPUT 이고 조회하지 않는다`() {
        val exception =
            assertThrows<BusinessException> {
                service.list(
                    CourseTracesQuery(userId = 1L, status = CourseTraceStatus.COMPLETED, cursor = "!!", size = 10),
                )
            }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        verifyNoInteractions(courseTracePersistencePort)
    }

    private fun completedRow(id: Long) =
        CourseTraceRow(
            id = id,
            userId = 1L,
            courseId = 1L,
            status = CourseTraceStatus.COMPLETED,
            startedAt = Instant.parse("2026-11-02T09:00:00Z"),
            endedAt = Instant.parse("2026-11-02T10:00:00Z"),
            durationMinutes = 60,
            distanceMeters = 1200,
        )

    private fun placeRef(
        id: Long,
        name: String,
        coordinate: Coordinate,
    ) = PlaceRef(id = id, category = "CAFE", areaCode = null, name = name, coordinate = coordinate)
}
