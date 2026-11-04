package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceDetailResult
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.course.application.port.inbound.dto.VisitedPlaceResult
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

class HomeTraceAdapterTest {
    private val courseTraceUseCase = mock(CourseTraceUseCase::class.java)
    private val adapter = HomeTraceAdapter(courseTraceUseCase)
    private val startedAt = Instant.parse("2026-11-03T05:30:00Z")

    @Test
    fun `진행 중 세션이 없으면 null 이다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(null)

        assertNull(adapter.findInProgress(1L))
    }

    @Test
    fun `세션과 코스 요약을 합치고 장소 수는 코스 장소 개수, 방문 수는 체크인 개수다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(detail(viewableCourse()))

        assertEquals(
            HomeTracingCourse(
                traceId = 10L,
                courseId = 3L,
                title = "을지로 노포 골목 투어",
                coverImageUrl = "https://img/cover.jpg",
                area = "을지로",
                placeCount = 2,
                visitedPlaceCount = 1,
                startedAt = startedAt,
                isDeleted = false,
            ),
            adapter.findInProgress(1L),
        )
    }

    @Test
    fun `코스를 더 볼 수 없어도 카드는 남기고 isDeleted 로 표시한다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(detail(TracedCourseSummary.deleted(3L)))

        val card = adapter.findInProgress(1L)

        assertEquals(true, card?.isDeleted)
        assertEquals(10L, card?.traceId)
        assertNull(card?.title)
        assertEquals(2, card?.placeCount)
    }

    private fun detail(course: TracedCourseSummary) =
        CourseTraceDetailResult(
            traceId = 10L,
            status = CourseTraceStatus.IN_PROGRESS,
            startedAt = startedAt,
            endedAt = null,
            durationMinutes = 0,
            distanceMeters = 0,
            course = course,
            places = listOf(coursePlace(101L, 0), coursePlace(102L, 1)),
            visitedPlaces = listOf(VisitedPlaceResult(placeId = 101L, visitedAt = startedAt)),
        )

    private fun viewableCourse() =
        TracedCourseSummary(
            courseId = 3L,
            title = "을지로 노포 골목 투어",
            coverImageUrl = "https://img/cover.jpg",
            area = "을지로",
            placeCount = 2,
            isDeleted = false,
        )

    private fun coursePlace(
        placeId: Long,
        orderNo: Int,
    ) = CoursePlaceResult(
        id = orderNo + 1L,
        placeId = placeId,
        orderNo = orderNo,
        caption = null,
        walkingMinutesToNext = null,
        images = emptyList(),
    )
}
