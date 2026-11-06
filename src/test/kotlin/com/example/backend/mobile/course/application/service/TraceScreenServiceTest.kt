package com.example.backend.mobile.course.application.service

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceDetailResult
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

/** [TraceScreenService] 단위 테스트 — 세션 판정은 course 도메인 몫이라 예외 전파와 장소 배치 조회만 본다. */
class TraceScreenServiceTest {
    private val courseTraceUseCase = mock(CourseTraceUseCase::class.java)
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val service = TraceScreenService(courseTraceUseCase, placeQueryUseCase)

    @Test
    fun `세션 상세와 장소 요약을 묶어 돌려준다 — 장소는 중복 없이 한 번에 조회한다`() {
        `when`(courseTraceUseCase.getDetail(USER_ID, TRACE_ID))
            .thenReturn(detail(placeIds = listOf(1L, 2L, 1L)))
        `when`(placeQueryUseCase.findPlacesById(listOf(1L, 2L))).thenReturn(listOf(place(1L), place(2L)))

        val screen = service.getScreen(TRACE_ID, USER_ID)

        assertEquals(TRACE_ID, screen.trace.traceId)
        assertEquals(listOf(1L, 2L), screen.places.map { it.id })
        verify(placeQueryUseCase).findPlacesById(listOf(1L, 2L))
    }

    @Test
    fun `세션 판정 예외는 그대로 올라오고 장소를 조회하지 않는다`() {
        `when`(courseTraceUseCase.getDetail(USER_ID, TRACE_ID))
            .thenThrow(BusinessException(CourseErrorCode.COURSE_TRACE_NOT_FOUND))

        val exception = assertThrows<BusinessException> { service.getScreen(TRACE_ID, USER_ID) }

        assertEquals(CourseErrorCode.COURSE_TRACE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(placeQueryUseCase)
    }

    private fun detail(placeIds: List<Long>) =
        CourseTraceDetailResult(
            traceId = TRACE_ID,
            status = CourseTraceStatus.IN_PROGRESS,
            startedAt = Instant.parse("2026-11-03T05:30:00Z"),
            endedAt = null,
            durationMinutes = 0,
            distanceMeters = 0,
            course = TracedCourseSummary.deleted(3L),
            places =
                placeIds.mapIndexed { index, placeId ->
                    CoursePlaceResult(
                        id = index + 1L,
                        placeId = placeId,
                        orderNo = index,
                        caption = null,
                        walkingMinutesToNext = null,
                        images = emptyList(),
                    )
                },
            visitedPlaces = emptyList(),
        )

    private fun place(id: Long) =
        PlaceSummary(
            id = id,
            name = "장소$id",
            category = "CAFE",
            imageUrl = null,
            latitude = 37.5,
            longitude = 127.0,
            address = "서울",
            areaCode = null,
        )

    private companion object {
        const val TRACE_ID = 10L
        const val USER_ID = 1L
    }
}
