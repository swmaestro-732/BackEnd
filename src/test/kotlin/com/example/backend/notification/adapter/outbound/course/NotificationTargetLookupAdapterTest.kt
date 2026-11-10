package com.example.backend.notification.adapter.outbound.course

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseOwnerSummary
import com.example.backend.notification.application.port.outbound.NotificationCourseTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class NotificationTargetLookupAdapterTest {
    private val courses = mock(CourseQueryUseCase::class.java)
    private val adapter = NotificationTargetLookupAdapter(courses)

    @Test
    fun `코스 소유자 요약에서 받는 사람과 제목과 커버 이미지를 반환한다`() {
        `when`(courses.findOwnerSummary(10L)).thenReturn(CourseOwnerSummary(10L, 7L, "한강 산책", "https://cdn/c.jpg"))

        assertThat(adapter.findCourse(10L)).isEqualTo(NotificationCourseTarget(7L, "한강 산책", "https://cdn/c.jpg"))
        verify(courses).findOwnerSummary(10L)
    }

    @Test
    fun `대상 코스가 없거나 비활성이면 null을 반환한다`() {
        `when`(courses.findOwnerSummary(10L)).thenReturn(null)

        assertThat(adapter.findCourse(10L)).isNull()
        verify(courses).findOwnerSummary(10L)
    }
}
