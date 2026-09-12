package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

/**
 * [CourseCountEventHandler] 단위 테스트 — 동기 반영 성공 시 use case 만 호출하고, 실패 시 SQS 폴백으로 재시도한다.
 */
class CourseCountEventHandlerTest {
    private val useCase = mock(CourseCountUseCase::class.java)
    private val fallbackPort = mock(CourseCountFallbackPort::class.java)
    private val handler = CourseCountEventHandler(useCase, fallbackPort)

    private fun event(
        authorId: Long = 1L,
        old: CourseVisibility? = null,
        new: CourseVisibility? = CourseVisibility.PUBLIC,
    ) = object : AuthorCourseCountChanged {
        override val authorId = authorId
        override val oldVisibility = old
        override val newVisibility = new
    }

    @Test
    fun `동기 반영에 성공하면 폴백하지 않는다`() {
        handler.onCourseCountChanged(event(authorId = 1L))

        verify(useCase).apply(anyString(), eq(1L), eq(null), eq(CourseVisibility.PUBLIC))
        verify(fallbackPort, never()).publish(anyLong(), any(), any(), anyString())
    }

    @Test
    fun `동기 반영이 실패하면 SQS 폴백으로 재시도한다`() {
        doThrow(RuntimeException("boom"))
            .`when`(useCase)
            .apply(anyString(), anyLong(), any(), any())

        handler.onCourseCountChanged(
            event(authorId = 5L, old = CourseVisibility.PUBLIC, new = CourseVisibility.PRIVATE),
        )

        verify(fallbackPort).publish(eq(5L), eq(CourseVisibility.PUBLIC), eq(CourseVisibility.PRIVATE), anyString())
    }

    private fun anyLong() = org.mockito.ArgumentMatchers.anyLong()

    private fun <T> any() = org.mockito.ArgumentMatchers.any<T>()
}
