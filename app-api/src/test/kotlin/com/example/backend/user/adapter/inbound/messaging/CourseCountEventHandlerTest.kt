package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class CourseCountEventHandlerTest {
    private val useCase = mock(CourseCountUseCase::class.java)
    private val fallback = mock(CourseCountFallbackPort::class.java)
    private val handler = CourseCountEventHandler(useCase, fallback)

    @Test
    fun `동기 반영 성공 시 UseCase 를 호출하고 폴백은 건드리지 않는다`() {
        handler.onCourseCountChanged(
            event(authorId = 1L, old = CourseVisibility.PUBLIC, new = CourseVisibility.PRIVATE),
        )

        verify(useCase).apply(anyString(), eq(1L), eq(CourseVisibility.PUBLIC), eq(CourseVisibility.PRIVATE))
        verifyNoInteractions(fallback)
    }

    @Test
    fun `동기 반영 실패 시 폴백 포트로 발행한다`() {
        doThrow(RuntimeException("DB 오류")).`when`(useCase).apply(anyString(), anyLong(), any(), any())

        handler.onCourseCountChanged(
            event(authorId = 2L, old = null, new = CourseVisibility.PUBLIC),
        )

        verify(fallback).publish(
            authorId = eq(2L),
            oldVisibility = eq(null),
            newVisibility = eq(CourseVisibility.PUBLIC),
            eventId = anyString(),
        )
    }

    private fun event(
        authorId: Long,
        old: CourseVisibility?,
        new: CourseVisibility?,
    ): AuthorCourseCountChanged =
        object : AuthorCourseCountChanged {
            override val authorId = authorId
            override val oldVisibility = old
            override val newVisibility = new
        }
}
