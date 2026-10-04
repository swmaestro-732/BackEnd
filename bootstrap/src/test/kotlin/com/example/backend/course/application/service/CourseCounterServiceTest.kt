package com.example.backend.course.application.service

import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CourseCounterServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val service = CourseCounterService(persistence)

    @Test
    fun `저장 수 증가 결과를 반환한다`() {
        `when`(persistence.increaseSavesCount(10L)).thenReturn(1)

        assertEquals(1, service.increaseSavesCount(10L))
    }

    @Test
    fun `저장 수 감소를 영속 포트에 위임한다`() {
        service.decreaseSavesCount(10L)

        verify(persistence).decreaseSavesCount(10L)
    }
}
