package com.example.backend.course.application.service

import com.example.backend.course.application.event.CourseCounter
import com.example.backend.course.application.event.CourseCounterChangedEvent
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher

class CourseCounterServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val service = CourseCounterService(persistence, eventPublisher)

    @Test
    fun `저장 수 증가 결과를 반환하고 delta +1 이벤트를 발행한다`() {
        `when`(persistence.increaseSavesCount(10L)).thenReturn(1)

        assertEquals(1, service.increaseSavesCount(10L))
        verify(eventPublisher).publishEvent(CourseCounterChangedEvent(10L, CourseCounter.SAVES, delta = 1))
    }

    @Test
    fun `저장 수 감소를 영속 포트에 위임하고 delta -1 이벤트를 발행한다`() {
        `when`(persistence.decreaseSavesCount(10L)).thenReturn(1)

        service.decreaseSavesCount(10L)

        verify(persistence).decreaseSavesCount(10L)
        verify(eventPublisher).publishEvent(CourseCounterChangedEvent(10L, CourseCounter.SAVES, delta = -1))
    }

    @Test
    fun `갱신된 행이 없으면 이벤트를 발행하지 않는다`() {
        `when`(persistence.increaseSavesCount(10L)).thenReturn(0)
        `when`(persistence.decreaseSavesCount(10L)).thenReturn(0)

        service.increaseSavesCount(10L)
        service.decreaseSavesCount(10L)

        verifyNoInteractions(eventPublisher)
    }
}
