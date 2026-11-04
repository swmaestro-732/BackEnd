package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.CourseTraceSummary
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class HomeTraceAdapterTest {
    private val courseTraceUseCase = mock(CourseTraceUseCase::class.java)
    private val courseQueryUseCase = mock(CourseQueryUseCase::class.java)
    private val adapter = HomeTraceAdapter(courseTraceUseCase, courseQueryUseCase)
    private val startedAt = Instant.parse("2026-11-03T05:30:00Z")

    @Test
    fun `진행 중 세션이 없으면 null 이고 코스를 조회하지 않는다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(null)

        assertNull(adapter.findInProgress(1L))
        verifyNoInteractions(courseQueryUseCase)
    }

    @Test
    fun `코스를 더 볼 수 없으면 null 이다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(summary())
        `when`(courseQueryUseCase.getDetails(listOf(3L), 1L)).thenReturn(emptyList())

        assertNull(adapter.findInProgress(1L))
    }

    @Test
    fun `세션과 코스 요약을 합치고 장소 수는 코스 장소 개수다`() {
        `when`(courseTraceUseCase.findInProgress(1L)).thenReturn(summary())
        `when`(courseQueryUseCase.getDetails(listOf(3L), 1L)).thenReturn(listOf(course()))

        assertEquals(
            HomeTracingCourse(
                traceId = 10L,
                courseId = 3L,
                title = "을지로 노포 골목 투어",
                coverImageUrl = "https://img/cover.jpg",
                area = "을지로",
                placeCount = 2,
                startedAt = startedAt,
            ),
            adapter.findInProgress(1L),
        )
    }

    private fun summary() =
        CourseTraceSummary(
            id = 10L,
            courseId = 3L,
            startedAt = startedAt,
            endedAt = null,
            durationMinutes = 0,
            distanceMeters = 0,
        )

    private fun course() =
        CourseDetailResult(
            id = 3L,
            title = "을지로 노포 골목 투어",
            coverImageUrl = "https://img/cover.jpg",
            theme = "FOOD",
            area = "을지로",
            tags = emptyList(),
            description = "",
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            authorId = 2L,
            tracingsCnt = 0,
            places = listOf(place(1L), place(2L)),
            hasSaved = false,
            hasLiked = false,
            hasStartedCourse = true,
        )

    private fun place(id: Long) =
        CoursePlaceResult(
            id = id,
            placeId = id,
            orderNo = id.toInt(),
            caption = null,
            walkingMinutesToNext = null,
            images = emptyList(),
        )
}
