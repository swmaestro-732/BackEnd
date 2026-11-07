package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.CoursePlaceStats
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.course.application.port.outbound.WalkingGuide
import com.example.backend.course.application.port.outbound.WalkingRoute
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import com.example.backend.course.domain.model.CourseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

/**
 * [CourseTraceService] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * - findRoute: 출발·도착 장소 검증(같으면 400, 없으면 404), 조회 순서 보정, 도보 경로 → 결과 DTO 변환, 도보 불가(null) 전파.
 * - findInProgress: 진행 중 세션 조회.
 * - listCompleted: 커서 디코딩 → beforeId 전달, size+1 조회로 hasNext 판정·페이지 절단·nextCursor 생성, 행 → Summary 변환.
 */
class CourseTraceServiceTest {
    private val courseTracePersistencePort = mock(CourseTracePersistencePort::class.java)
    private val coursePersistencePort = mock(CoursePersistencePort::class.java)
    private val courseViewPolicy = mock(CourseViewPolicy::class.java)
    private val placeLookupPort = mock(PlaceLookupPort::class.java)
    private val walkingRoutePort = mock(WalkingRoutePort::class.java)
    private val service =
        CourseTraceService(
            courseTracePersistencePort = courseTracePersistencePort,
            coursePersistencePort = coursePersistencePort,
            courseViewPolicy = courseViewPolicy,
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

    // --- listCompleted ---

    @Test
    fun `목록 — size 보다 많이 조회되면 잘라내고 hasNext 와 마지막 행 id 커서를 돌려준다`() {
        `when`(courseTracePersistencePort.findCompleted(1L, null, 3))
            .thenReturn(listOf(completedRow(30L), completedRow(20L), completedRow(10L)))

        val result = service.listCompleted(CourseTracesQuery(userId = 1L, cursor = null, size = 2))

        assertEquals(true, result.hasNext)
        assertEquals(listOf(30L, 20L), result.traces.map { it.trace.id })
        assertEquals(20L, CourseTraceCursorCodec.decode(result.nextCursor))
    }

    @Test
    fun `목록 — 커서를 beforeId 로 풀어 넘기고 마지막 페이지면 nextCursor 가 null 이다`() {
        `when`(courseTracePersistencePort.findCompleted(1L, 20L, 3))
            .thenReturn(listOf(completedRow(10L)))

        val result =
            service.listCompleted(
                CourseTracesQuery(
                    userId = 1L,
                    cursor = CourseTraceCursorCodec.encode(20L),
                    size = 2,
                ),
            )

        assertEquals(false, result.hasNext)
        assertNull(result.nextCursor)
        assertEquals(listOf(10L), result.traces.map { it.trace.id })
    }

    @Test
    fun `목록 — 세션 행을 시각·소요 시간·거리 그대로 Summary 로 옮긴다`() {
        `when`(courseTracePersistencePort.findCompleted(1L, null, 11)).thenReturn(listOf(completedRow(5L)))

        val summary =
            service
                .listCompleted(CourseTracesQuery(userId = 1L, cursor = null, size = 10))
                .traces
                .single()
                .trace

        assertEquals(
            CourseTraceSummary(
                id = 5L,
                courseId = 1L,
                startedAt = Instant.parse("2026-11-02T09:00:00Z"),
                endedAt = Instant.parse("2026-11-02T10:00:00Z"),
                durationMinutes = 60,
                distanceMeters = 1200,
            ),
            summary,
        )
    }

    @Test
    fun `목록 — 볼 수 있는 코스는 요약을 채우고 코스 id 를 중복 없이 한 번에 조회한다`() {
        `when`(courseTracePersistencePort.findCompleted(1L, null, 11))
            .thenReturn(listOf(completedRow(20L), completedRow(10L)))
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(listOf(courseRow(CourseStatus.ACTIVE)))
        `when`(courseViewPolicy.isViewable(CourseVisibility.PUBLIC, 2L, 1L)).thenReturn(true)
        `when`(coursePersistencePort.findPlaceStats(listOf(1L))).thenReturn(mapOf(1L to CoursePlaceStats(4, 30)))

        val courses =
            service.listCompleted(CourseTracesQuery(userId = 1L, cursor = null, size = 10)).traces.map { it.course }

        val expected =
            TracedCourseSummary(
                courseId = 1L,
                title = "성수 코스",
                coverImageUrl = "cover.jpg",
                area = "성수",
                placeCount = 4,
                isDeleted = false,
            )
        assertEquals(listOf(expected, expected), courses)
    }

    @Test
    fun `목록 — 삭제됐거나 볼 수 없는 코스는 courseId 외 null 이고 isDeleted 다`() {
        `when`(courseTracePersistencePort.findCompleted(1L, null, 11)).thenReturn(listOf(completedRow(10L)))
        // 삭제된 코스는 조회 결과에서 빠지고, 비활성·비공개 전환 코스는 조회돼도 걸러진다.
        listOf(emptyList(), listOf(courseRow(CourseStatus.DELETED)), listOf(courseRow(CourseStatus.ACTIVE))).forEach {
            `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(it)

            val course =
                service
                    .listCompleted(CourseTracesQuery(userId = 1L, cursor = null, size = 10))
                    .traces
                    .single()
                    .course

            assertEquals(TracedCourseSummary.deleted(1L), course)
        }
    }

    // --- getDetail ---

    @Test
    fun `상세 — 세션·코스 요약·코스 장소·체크인을 묶어 돌려준다`() {
        val row = completedRow(5L).copy(status = CourseTraceStatus.IN_PROGRESS, endedAt = null)
        val visited = listOf(VisitedPlaceResult(placeId = 22L, visitedAt = row.startedAt))
        `when`(courseTracePersistencePort.readById(5L)).thenReturn(row)
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(listOf(courseRow(CourseStatus.ACTIVE)))
        `when`(courseViewPolicy.isViewable(CourseVisibility.PUBLIC, 2L, 1L)).thenReturn(true)
        `when`(coursePersistencePort.findPlaceStats(listOf(1L))).thenReturn(mapOf(1L to CoursePlaceStats(2, 6)))
        `when`(coursePersistencePort.findPlaces(1L)).thenReturn(listOf(coursePlaceRow(22L, 0), coursePlaceRow(23L, 1)))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(visited)

        val detail = service.getDetail(1L, 5L)

        assertEquals(5L, detail.traceId)
        assertEquals(CourseTraceStatus.IN_PROGRESS, detail.status)
        assertEquals("성수 코스", detail.course.title)
        assertEquals(false, detail.course.isDeleted)
        assertEquals(listOf(22L to 0, 23L to 1), detail.places.map { it.placeId to it.orderNo })
        assertEquals(visited, detail.visitedPlaces)
    }

    @Test
    fun `상세 — 코스가 삭제됐어도 장소는 내려주고 요약만 isDeleted 다`() {
        val row = completedRow(5L).copy(status = CourseTraceStatus.IN_PROGRESS, endedAt = null)
        `when`(courseTracePersistencePort.readById(5L)).thenReturn(row)
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(emptyList())
        `when`(coursePersistencePort.findPlaces(1L)).thenReturn(listOf(coursePlaceRow(22L, 0)))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(emptyList())

        val detail = service.getDetail(1L, 5L)

        assertEquals(TracedCourseSummary.deleted(1L), detail.course)
        assertEquals(listOf(22L), detail.places.map { it.placeId })
    }

    @Test
    fun `상세 — 완주 세션은 종료 시각·소요 시간·거리와 함께 돌려주고 잠금 없이 읽는다`() {
        `when`(courseTracePersistencePort.readById(5L)).thenReturn(completedRow(5L))
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(emptyList())
        `when`(coursePersistencePort.findPlaces(1L)).thenReturn(emptyList())
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(emptyList())

        val detail = service.getDetail(1L, 5L)

        assertEquals(CourseTraceStatus.COMPLETED, detail.status)
        assertEquals(Instant.parse("2026-11-02T10:00:00Z"), detail.endedAt)
        assertEquals(60, detail.durationMinutes)
        assertEquals(1200, detail.distanceMeters)
        verify(courseTracePersistencePort, never()).findById(anyLong())
    }

    @Test
    fun `상세 — 없거나 타인의 세션은 404 다`() {
        `when`(courseTracePersistencePort.readById(6L)).thenReturn(completedRow(6L).copy(userId = 9L))
        `when`(courseTracePersistencePort.readById(7L)).thenReturn(null)

        listOf(6L, 7L).forEach { traceId ->
            assertEquals(
                CourseErrorCode.COURSE_TRACE_NOT_FOUND,
                assertThrows<BusinessException> { service.getDetail(1L, traceId) }.errorCode,
            )
        }
    }

    @Test
    fun `진행 중 — 진행 중 세션을 체크인 목록과 함께 돌려주고 없으면 null 이다`() {
        val row = completedRow(5L).copy(status = CourseTraceStatus.IN_PROGRESS, endedAt = null)
        val visited = listOf(VisitedPlaceResult(placeId = 22L, visitedAt = row.startedAt))
        `when`(courseTracePersistencePort.findInProgress(1L)).thenReturn(row)
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(visited)
        `when`(courseTracePersistencePort.findInProgress(2L)).thenReturn(null)

        val result = service.findInProgress(1L)

        assertEquals(5L, result?.traceId)
        assertEquals(visited, result?.visitedPlaces)
        assertNull(service.findInProgress(2L))
    }

    @Test
    fun `목록 — 잘못된 커서는 INVALID_INPUT 이고 조회하지 않는다`() {
        val exception =
            assertThrows<BusinessException> {
                service.listCompleted(CourseTracesQuery(userId = 1L, cursor = "!!", size = 10))
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

    private fun courseRow(status: CourseStatus) =
        CourseDetailRow(
            id = 1L,
            userId = 2L,
            title = "성수 코스",
            coverImageUrl = "cover.jpg",
            description = null,
            categories = emptyList(),
            area = "성수",
            areaCode = null,
            tracingsCnt = 0,
            status = status,
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            duplicatedFromId = null,
        )

    private fun coursePlaceRow(
        placeId: Long,
        orderNo: Int,
    ) = CoursePlaceRow(
        id = orderNo + 1L,
        placeId = placeId,
        orderNo = orderNo,
        caption = null,
        walkingMinutes = null,
        images = emptyList(),
    )

    private fun placeRef(
        id: Long,
        name: String,
        coordinate: Coordinate,
    ) = PlaceRef(id = id, category = "CAFE", areaCode = null, name = name, coordinate = coordinate)
}
