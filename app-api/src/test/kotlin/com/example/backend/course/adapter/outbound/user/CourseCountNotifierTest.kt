package com.example.backend.course.adapter.outbound.user

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountIntakeUseCase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class CourseCountNotifierTest {
    private val courseCountIntakeUseCase = mock(CourseCountIntakeUseCase::class.java)
    private val notifier = CourseCountNotifier(courseCountIntakeUseCase)

    @Test
    fun `공개범위 전이를 user 의 inbound 포트로 그대로 넘긴다`() {
        notifier.onCourseCountChanged(
            object : AuthorCourseCountChanged {
                override val authorId = 3L
                override val oldVisibility = CourseVisibility.PRIVATE
                override val newVisibility = CourseVisibility.PUBLIC
            },
        )

        verify(courseCountIntakeUseCase).receive(3L, CourseVisibility.PRIVATE, CourseVisibility.PUBLIC)
    }
}
