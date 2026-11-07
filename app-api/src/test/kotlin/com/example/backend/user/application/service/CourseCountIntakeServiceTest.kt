package com.example.backend.user.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        var fail = false

        override fun publish(
            authorId: Long,
            oldVisibility: CourseVisibility?,
            newVisibility: CourseVisibility?,
            eventId: String,
        ) {
            calls += listOf(eventId, authorId, oldVisibility, newVisibility)
            if (fail) throw RuntimeException("SQS 오류")
        }
    }

    @Test
    fun `동기 반영 성공 시 UseCase 를 호출하고 폴백은 건드리지 않는다`() {
        val useCase = RecordingUseCase(fail = false)
        val fallback = RecordingFallback()

        CourseCountIntakeService(useCase, fallback).receive("e1", 1L, CourseVisibility.PUBLIC, CourseVisibility.PRIVATE)

        assertThat(useCase.calls).containsExactly(listOf("e1", 1L, CourseVisibility.PUBLIC, CourseVisibility.PRIVATE))
        assertThat(fallback.calls).isEmpty()
    }

    @Test
    fun `동기 반영 실패 시 받은 eventId 그대로 폴백 포트에 발행한다`() {
        val useCase = RecordingUseCase(fail = true)
        val fallback = RecordingFallback()

        CourseCountIntakeService(useCase, fallback).receive("e2", 2L, null, CourseVisibility.PUBLIC)

        assertThat(fallback.calls).containsExactly(listOf("e2", 2L, null, CourseVisibility.PUBLIC))
        assertThat(fallback.calls[0]).isEqualTo(useCase.calls[0])
    }

    @Test
    fun `동기 반영과 폴백 발행이 모두 실패하면 예외를 던져 이벤트 재발행에 맡긴다`() {
        val useCase = RecordingUseCase(fail = true)
        val fallback = RecordingFallback().apply { fail = true }

        assertThatThrownBy {
            CourseCountIntakeService(useCase, fallback).receive("e3", 3L, null, CourseVisibility.PUBLIC)
        }.hasMessage("SQS 오류")
    }
}
