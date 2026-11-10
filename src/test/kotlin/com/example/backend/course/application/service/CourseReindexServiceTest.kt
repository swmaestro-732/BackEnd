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
        `when`(persistence.findForIndex(null, 100)).thenReturn(emptyList())

        assertEquals(0, service.reindexAll())
        verifyNoInteractions(index)
    }

    @Test
    fun `페이지 마지막 id를 커서로 사용해 공개 코스만 색인한다`() {
        val first = (1L..100L).map { course(it) }
        val visible = course(101L)
        // 숨김, 비공개 코스는 색인하지 않지만 커서는 페이지 마지막 id 로 넘어간다.
        val hidden = course(102L, publiclyVisible = false)
        `when`(persistence.findForIndex(null, 100)).thenReturn(first)
        `when`(persistence.findForIndex(100L, 100)).thenReturn(listOf(visible, hidden))

        assertEquals(101, service.reindexAll())
        verify(index).save(first)
        verify(index).save(listOf(visible))
    }

    private fun course(
        id: Long,
        publiclyVisible: Boolean = true,
    ): Course =
        mock(Course::class.java).also {
            `when`(it.id).thenReturn(id)
            `when`(it.isPubliclyVisible).thenReturn(publiclyVisible)
        }
}
