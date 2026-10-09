package com.example.backend.course.application.service

import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class CourseReindexServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val index = mock(CourseSearchIndexPort::class.java)
    private val service = CourseReindexService(persistence, index)

    @Test
    fun `코스가 없으면 색인하지 않는다`() {
        `when`(persistence.findForIndex(null, 500)).thenReturn(emptyList())

        assertEquals(0, service.reindexAll())
        verifyNoInteractions(index)
    }

    @Test
    fun `페이지 마지막 id를 커서로 사용해 전부 색인한다`() {
        val first = (1L..500L).map(::course)
        val second = listOf(course(501L), course(502L))
        `when`(persistence.findForIndex(null, 500)).thenReturn(first)
        `when`(persistence.findForIndex(500L, 500)).thenReturn(second)

        assertEquals(502, service.reindexAll())
        verify(index).save(first)
        verify(index).save(second)
    }

    private fun course(id: Long): Course =
        mock(Course::class.java).also {
            `when`(it.id).thenReturn(id)
        }
}
