package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.event.CourseActivityEvent
import com.example.backend.course.application.port.inbound.CourseActivityKind
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import com.example.backend.course.application.port.outbound.CourseTraceRow
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.WalkingRoutePort
import com.example.backend.course.domain.model.CourseTraceStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CourseTraceCompletionEventTest {
    private val traces = mock(CourseTracePersistencePort::class.java)
    private val courses = mock(CoursePersistencePort::class.java)
    private val viewPolicy = mock(CourseViewPolicy::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val occurredAt = Instant.parse("2026-10-24T10:09:59Z")
    private val clock = Clock.fixed(occurredAt, ZoneOffset.UTC)
    private val service =
        CourseTraceService(
            traces,
            courses,
            viewPolicy,
            mock(PlaceLookupPort::class.java),
            mock(WalkingRoutePort::class.java),
            eventPublisher,
            clock,
        )
    private val command = EndCourseTraceCommand(7L, 30L, emptyList(), 20, 1200)
    private val trace = CourseTraceRow(30L, 7L, 10L, CourseTraceStatus.IN_PROGRESS, Instant.EPOCH)

    @Test
    fun `따라가기 종료와 카운터 증가가 성공한 뒤 완주 이벤트를 한 번 발행한다`() {
        `when`(traces.findById(30L)).thenReturn(trace)
        `when`(traces.complete(30L, 20, 1200)).thenReturn(1)
        `when`(courses.increaseTracingsCount(10L)).thenReturn(1)

        service.end(command)

        val order = inOrder(traces, courses, eventPublisher)
        order.verify(traces).complete(30L, 20, 1200)
        order.verify(courses).increaseTracingsCount(10L)
        order
            .verify(
                eventPublisher,
            ).publishEvent(CourseActivityEvent(CourseActivityKind.TRACE_COMPLETED, 7L, 10L, occurredAt))
        verifyNoMoreInteractions(eventPublisher)
    }

    @Test
    fun `종료 갱신이 실패하면 카운터와 이벤트를 변경하지 않는다`() {
        `when`(traces.findById(30L)).thenReturn(trace)
        `when`(traces.complete(30L, 20, 1200)).thenReturn(0)

        val exception = assertThrows<BusinessException> { service.end(command) }

        assertEquals(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, exception.errorCode)
        verifyNoInteractions(courses, eventPublisher)
    }

    @Test
    fun `완주 카운터를 증가시키지 못하면 이벤트를 발행하지 않는다`() {
        `when`(traces.findById(30L)).thenReturn(trace)
        `when`(traces.complete(30L, 20, 1200)).thenReturn(1)
        `when`(courses.increaseTracingsCount(10L)).thenReturn(0)

        val exception = assertThrows<BusinessException> { service.end(command) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `이미 종료한 세션이면 완료를 반복하거나 이벤트를 발행하지 않는다`() {
        `when`(traces.findById(30L)).thenReturn(trace.copy(status = CourseTraceStatus.COMPLETED))

        val exception = assertThrows<BusinessException> { service.end(command) }

        assertEquals(CourseErrorCode.COURSE_TRACE_ALREADY_ENDED, exception.errorCode)
        verify(traces, never()).complete(30L, 20, 1200)
        verifyNoInteractions(courses, eventPublisher)
    }

    @Test
    fun `타인의 세션을 종료하려고 하면 이벤트를 발행하지 않는다`() {
        `when`(traces.findById(30L)).thenReturn(trace.copy(userId = 8L))

        val exception = assertThrows<BusinessException> { service.end(command) }

        assertEquals(CourseErrorCode.COURSE_TRACE_NOT_FOUND, exception.errorCode)
        verify(traces, never()).complete(30L, 20, 1200)
        verifyNoInteractions(courses, eventPublisher)
    }
}
