package com.example.backend.notification.adapter.outbound.lookup

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
    fun `코스 소유자 요약에서 받는 사람과 제목을 반환한다`() {
        `when`(courses.findOwnerSummaries(listOf(10L)))
            .thenReturn(mapOf(10L to CourseOwnerSummary(10L, 7L, "한강 산책")))

        assertThat(adapter.findCourse(10L)).isEqualTo(NotificationCourseTarget(7L, "한강 산책"))
        verify(courses).findOwnerSummaries(listOf(10L))
    }

    @Test
    fun `대상 코스가 조회 결과에 없으면 null을 반환한다`() {
        `when`(courses.findOwnerSummaries(listOf(10L))).thenReturn(emptyMap())

        assertThat(adapter.findCourse(10L)).isNull()
        verify(courses).findOwnerSummaries(listOf(10L))
    }
}
