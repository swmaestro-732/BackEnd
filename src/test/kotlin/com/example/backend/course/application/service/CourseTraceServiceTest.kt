package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.dto.CheckInPlaceCommand
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.course.application.port.inbound.dto.CourseTracesQuery
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceCommand
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.course.application.port.outbound.CourseDetailRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.inOrder
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
 * - startWithRoute·changeRoute·checkIn·end: 루트 세션 규칙 — 장소 검증 뒤 루트 저장, 이어가기면 루트 유지, 루트 기준 체크인 검증,
 *   재전송(같은 장소·같은 시각)·담긴 횟수 초과 무시, 남은 루트 교체.
 * - getDetail: 루트 세션의 장소 매핑(코스 캡션·사진 짝짓기, 코스 밖 장소, 도보 시간 유지 조건).
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
    private val visitedAt1 = Instant.parse("2026-10-10T05:00:00Z")
    private val visitedAt2 = Instant.parse("2026-10-10T06:00:00Z")
    private val visitedAt3 = Instant.parse("2026-10-10T07:00:00Z")
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

    // --- startWithRoute ---

    @Test
    fun `시작 V2 — 코스를 확인하고 장소를 검증한 뒤 세션을 만들고 받은 순서대로 루트를 저장한다`() {
        stubViewableCourse()
        `when`(placeLookupPort.findPlacesByIds(listOf(23L, 24L, 22L))).thenReturn(listOf(ref(23L), ref(24L), ref(22L)))
        `when`(courseTracePersistencePort.findInProgress(1L)).thenReturn(null)
        `when`(courseTracePersistencePort.insertInProgress(1L, 1L)).thenReturn(5L)
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(23L, 24L, 22L))

        val result = service.startWithRoute(1L, 1L, listOf(23L, 24L, 22L))

        assertEquals(5L, result.traceId)
        assertEquals(listOf(23L, 24L, 22L), result.placeIds)
        assertEquals(emptyList<VisitedPlaceResult>(), result.visitedPlaces)
        inOrder(courseTracePersistencePort).apply {
            verify(courseTracePersistencePort).insertInProgress(1L, 1L)
            verify(courseTracePersistencePort).insertRoutePlaces(5L, listOf(23L, 24L, 22L))
        }
    }

    @Test
    fun `시작 V2 — 같은 장소가 두 번이면 장소는 한 번만 조회하고 루트는 받은 그대로 저장한다`() {
        stubViewableCourse()
        `when`(placeLookupPort.findPlacesByIds(listOf(22L, 24L))).thenReturn(listOf(ref(22L), ref(24L)))
        `when`(courseTracePersistencePort.findInProgress(1L)).thenReturn(null)
        `when`(courseTracePersistencePort.insertInProgress(1L, 1L)).thenReturn(5L)
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))

        service.startWithRoute(1L, 1L, listOf(22L, 24L, 22L))

        verify(placeLookupPort).findPlacesByIds(listOf(22L, 24L))
        verify(courseTracePersistencePort).insertRoutePlaces(5L, listOf(22L, 24L, 22L))
    }

    @Test
    fun `시작 V2 — 없는 장소가 있으면 PLACE_NOT_FOUND 이고 세션을 만들지 않는다`() {
        stubViewableCourse()
        `when`(placeLookupPort.findPlacesByIds(listOf(22L, 99L))).thenReturn(listOf(ref(22L)))

        val exception = assertThrows<BusinessException> { service.startWithRoute(1L, 1L, listOf(22L, 99L)) }

        assertEquals(PlaceErrorCode.PLACE_NOT_FOUND, exception.errorCode)
        verify(courseTracePersistencePort, never()).insertInProgress(anyLong(), anyLong())
    }

    @Test
    fun `시작 V2 — 같은 코스를 진행 중이면 요청 루트는 검증도 저장도 하지 않고 기존 세션과 그 루트를 돌려준다`() {
        stubViewableCourse()
        `when`(courseTracePersistencePort.findInProgress(1L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(23L, 24L))

        // 앱 재시작 뒤 지워진 장소가 든 옛 placeIds 로 다시 시작해도 이어가기는 성공해야 한다.
        val result = service.startWithRoute(1L, 1L, listOf(99L))

        assertEquals(5L, result.traceId)
        assertEquals(listOf(23L, 24L), result.placeIds)
        verifyNoInteractions(placeLookupPort)
        verify(courseTracePersistencePort, never()).insertInProgress(anyLong(), anyLong())
        verify(courseTracePersistencePort, never()).insertRoutePlaces(anyLong(), anyList())
    }

    @Test
    fun `시작 V2 — 다른 코스를 진행 중이면 장소 검증보다 먼저 COURSE_TRACE_ALREADY_IN_PROGRESS 다`() {
        stubViewableCourse()
        `when`(courseTracePersistencePort.findInProgress(1L)).thenReturn(inProgressRow(5L).copy(courseId = 4L))

        val exception = assertThrows<BusinessException> { service.startWithRoute(1L, 1L, listOf(99L)) }

        assertEquals(CourseErrorCode.COURSE_TRACE_ALREADY_IN_PROGRESS, exception.errorCode)
        verifyNoInteractions(placeLookupPort)
    }

    // --- checkIn · end (루트 세션) ---

    @Test
    fun `체크인 — 루트 세션은 코스 장소를 보지 않고 루트 기준으로 검증한 뒤 방문을 기록한다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(22L, 24L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(emptyList())

        service.checkIn(CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 24L, visitedAt = visitedAt1))
        val exception =
            assertThrows<BusinessException> {
                service.checkIn(CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 23L, visitedAt = visitedAt1))
            }

        verify(courseTracePersistencePort).insertVisitedPlace(5L, 24L, visitedAt1)
        assertEquals(CourseErrorCode.PLACE_NOT_IN_COURSE, exception.errorCode)
        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 23L, visitedAt1)
        verify(coursePersistencePort, never()).findPlaces(anyLong())
    }

    @Test
    fun `체크인 — 같은 장소·같은 시각은 재전송이라 기록하지 않고 마이크로초 아래는 같은 시각으로 본다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(22L, 24L, 22L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(listOf(VisitedPlaceResult(22L, visitedAt1)))

        service.checkIn(CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 22L, visitedAt = visitedAt1))
        service.checkIn(
            CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 22L, visitedAt = visitedAt1.plusNanos(500)),
        )

        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 22L, visitedAt1)
        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 22L, visitedAt1.plusNanos(500))
    }

    @Test
    fun `체크인 — 시각이 다르면 다음 방문으로 기록하고 담긴 횟수를 넘으면 기록하지 않는다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(22L, 24L, 22L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L))
            .thenReturn(listOf(VisitedPlaceResult(22L, visitedAt1)))
            .thenReturn(listOf(VisitedPlaceResult(22L, visitedAt1), VisitedPlaceResult(22L, visitedAt2)))

        service.checkIn(CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 22L, visitedAt = visitedAt2))
        service.checkIn(CheckInPlaceCommand(userId = 1L, traceId = 5L, placeId = 22L, visitedAt = visitedAt3))

        verify(courseTracePersistencePort).insertVisitedPlace(5L, 22L, visitedAt2)
        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 22L, visitedAt3)
    }

    @Test
    fun `종료 — 병합 체크인은 한 요청 안에서도 재전송·횟수 초과를 걸러 새 방문만 기록하고 완주 처리한다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(22L, 24L, 22L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(listOf(VisitedPlaceResult(22L, visitedAt1)))
        `when`(courseTracePersistencePort.complete(5L, 90, 1500)).thenReturn(1)

        service.end(
            EndCourseTraceCommand(
                userId = 1L,
                traceId = 5L,
                visitedPlaces =
                    listOf(
                        VisitedPlaceCommand(22L, visitedAt1),
                        VisitedPlaceCommand(22L, visitedAt2),
                        VisitedPlaceCommand(22L, visitedAt3),
                        VisitedPlaceCommand(24L, visitedAt3),
                    ),
                durationMinutes = 90,
                distanceMeters = 1500,
            ),
        )

        verify(courseTracePersistencePort).insertVisitedPlace(5L, 22L, visitedAt2)
        verify(courseTracePersistencePort).insertVisitedPlace(5L, 24L, visitedAt3)
        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 22L, visitedAt1)
        verify(courseTracePersistencePort, never()).insertVisitedPlace(5L, 22L, visitedAt3)
        verify(coursePersistencePort).increaseTracingsCount(1L)
    }

    // --- changeRoute ---

    @Test
    fun `루트 변경 — 장소를 검증하고 남은 루트를 받은 그대로 바꾼다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(placeLookupPort.findPlacesByIds(listOf(24L, 22L))).thenReturn(listOf(ref(24L), ref(22L)))

        service.changeRoute(1L, 5L, listOf(24L, 22L, 24L))

        verify(courseTracePersistencePort).replaceRemainingRoutePlaces(5L, listOf(24L, 22L, 24L))
    }

    @Test
    fun `루트 변경 — 체크인한 곳이 없는데 빈 루트면 INVALID_INPUT 이고 바꾸지 않으며, 체크인이 있으면 비울 수 있다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L))
            .thenReturn(emptyList())
            .thenReturn(listOf(VisitedPlaceResult(22L, visitedAt1)))

        val exception = assertThrows<BusinessException> { service.changeRoute(1L, 5L, emptyList()) }
        service.changeRoute(1L, 5L, emptyList())

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        verify(courseTracePersistencePort).replaceRemainingRoutePlaces(5L, emptyList())
    }

    @Test
    fun `루트 변경 — 종료된 세션은 COURSE_TRACE_ALREADY_ENDED 이고 없는 장소는 PLACE_NOT_FOUND 다`() {
        `when`(courseTracePersistencePort.findById(5L)).thenReturn(inProgressRow(5L))
        `when`(courseTracePersistencePort.findById(6L)).thenReturn(completedRow(6L))
        `when`(placeLookupPort.findPlacesByIds(listOf(99L))).thenReturn(emptyList())

        assertEquals(
            CourseErrorCode.COURSE_TRACE_ALREADY_ENDED,
            assertThrows<BusinessException> { service.changeRoute(1L, 6L, listOf(22L)) }.errorCode,
        )
        assertEquals(
            PlaceErrorCode.PLACE_NOT_FOUND,
            assertThrows<BusinessException> { service.changeRoute(1L, 5L, listOf(99L)) }.errorCode,
        )
        verify(courseTracePersistencePort, never()).replaceRemainingRoutePlaces(anyLong(), anyList())
    }

    // --- getDetail (루트 세션) ---

    @Test
    fun `상세 — 루트 세션은 루트 순서대로 코스 장소의 캡션·사진을 붙이고 코스 밖 장소는 비우며 도보 시간은 다음 장소가 코스와 같을 때만 남긴다`() {
        `when`(courseTracePersistencePort.readById(5L)).thenReturn(inProgressRow(5L))
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(emptyList())
        `when`(coursePersistencePort.findPlaces(1L)).thenReturn(
            listOf(
                coursePlaceRow(
                    22L,
                    0,
                ).copy(caption = "첫 장소", walkingMinutes = 6, images = listOf(CoursePlaceImageRow("a.jpg", 0))),
                coursePlaceRow(23L, 1).copy(walkingMinutes = 4),
                coursePlaceRow(25L, 2),
            ),
        )
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(23L, 25L, 24L, 22L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(emptyList())

        val places = service.getDetail(1L, 5L).places

        assertEquals(listOf(23L, 25L, 24L, 22L), places.map { it.placeId })
        assertEquals(listOf(0, 1, 2, 3), places.map { it.orderNo })
        // 23 → 25 는 코스에서도 이어지는 구간이라 도보 시간을 남기고, 22 는 다음이 코스(23)와 달라 비운다.
        assertEquals(listOf(4, null, null, null), places.map { it.walkingMinutesToNext })
        assertEquals(listOf(2L, 3L, 0L, 1L), places.map { it.id })
        assertEquals("첫 장소", places[3].caption)
        assertEquals(listOf("a.jpg"), places[3].images.map { it.imageUrl })
        assertNull(places[2].caption)
        assertEquals(emptyList<CoursePlaceImageResult>(), places[2].images)
    }

    @Test
    fun `상세 — 코스에 같은 장소가 여러 번이면 루트의 n 번째 항목에 코스의 n 번째 항목을 짝짓고 넘치면 캡션 없이 사진만 붙인다`() {
        `when`(courseTracePersistencePort.readById(5L)).thenReturn(inProgressRow(5L))
        `when`(coursePersistencePort.findCourseDetails(listOf(1L))).thenReturn(emptyList())
        `when`(coursePersistencePort.findPlaces(1L)).thenReturn(
            listOf(
                coursePlaceRow(22L, 0).copy(caption = "첫 방문"),
                coursePlaceRow(23L, 1),
                coursePlaceRow(22L, 2).copy(caption = "두 번째 방문", images = listOf(CoursePlaceImageRow("second.jpg", 0))),
            ),
        )
        `when`(courseTracePersistencePort.findRoutePlaceIds(5L)).thenReturn(listOf(22L, 22L, 22L))
        `when`(courseTracePersistencePort.findVisitedPlaces(5L)).thenReturn(emptyList())

        val places = service.getDetail(1L, 5L).places

        assertEquals(listOf("첫 방문", "두 번째 방문", null), places.map { it.caption })
        assertEquals(0L, places[2].id)
        assertEquals(listOf("second.jpg"), places[2].images.map { it.imageUrl })
    }

    private fun stubViewableCourse() {
        `when`(coursePersistencePort.findCourseDetail(1L)).thenReturn(courseRow(CourseStatus.ACTIVE))
        `when`(courseViewPolicy.isViewable(CourseVisibility.PUBLIC, 2L, 1L)).thenReturn(true)
    }

    private fun inProgressRow(id: Long) = completedRow(id).copy(status = CourseTraceStatus.IN_PROGRESS, endedAt = null)

    private fun ref(id: Long) = placeRef(id, "장소 $id", Coordinate(latitude = 37.5, longitude = 127.0))

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
