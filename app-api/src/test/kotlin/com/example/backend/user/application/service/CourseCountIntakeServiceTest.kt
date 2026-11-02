package com.example.backend.user.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CourseCountIntakeServiceTest {
    private class RecordingUseCase(
        private val fail: Boolean,
    ) : CourseCountUseCase {
        val calls = mutableListOf<List<Any?>>()

        override fun apply(
            eventId: String,
            authorId: Long,
            oldVisibility: CourseVisibility?,
            newVisibility: CourseVisibility?,
        ) {
            calls += listOf(eventId, authorId, oldVisibility, newVisibility)
            if (fail) throw RuntimeException("DB 오류")
        }
    }

    private class RecordingFallback : CourseCountFallbackPort {
        val calls = mutableListOf<List<Any?>>()

        override fun publish(
            authorId: Long,
            oldVisibility: CourseVisibility?,
            newVisibility: CourseVisibility?,
            eventId: String,
        ) {
            calls += listOf(eventId, authorId, oldVisibility, newVisibility)
        }
    }

    @Test
    fun `동기 반영 성공 시 UseCase 를 호출하고 폴백은 건드리지 않는다`() {
        val useCase = RecordingUseCase(fail = false)
        val fallback = RecordingFallback()

        CourseCountIntakeService(useCase, fallback).receive(1L, CourseVisibility.PUBLIC, CourseVisibility.PRIVATE)

        assertThat(useCase.calls).hasSize(1)
        assertThat(useCase.calls[0].drop(1)).containsExactly(1L, CourseVisibility.PUBLIC, CourseVisibility.PRIVATE)
        assertThat(fallback.calls).isEmpty()
    }

    @Test
    fun `동기 반영 실패 시 같은 eventId 로 폴백 포트에 발행한다`() {
        val useCase = RecordingUseCase(fail = true)
        val fallback = RecordingFallback()

        CourseCountIntakeService(useCase, fallback).receive(2L, null, CourseVisibility.PUBLIC)

        assertThat(fallback.calls).hasSize(1)
        assertThat(fallback.calls[0]).isEqualTo(useCase.calls[0])
    }
}
