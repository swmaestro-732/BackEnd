package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.adapter.messaging.CourseCountMessage
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * [CourseCountSqsConsumer] 단위 테스트 — 폴백 큐 메시지를 use case 로 위임하며 공개범위 문자열을 파싱한다.
 * null 은 통과, 미정의 값은 예외(ack 안 함 → 재처리)로 드러나는지 확인한다.
 */
class CourseCountSqsConsumerTest {
    private val useCase = mock(CourseCountUseCase::class.java)
    private val consumer = CourseCountSqsConsumer(useCase)

    @Test
    fun `유효한 공개범위를 파싱해 use case 로 위임한다`() {
        consumer.onMessage(
            CourseCountMessage(authorId = 1L, oldVisibility = "PRIVATE", newVisibility = "PUBLIC", eventId = "e1"),
        )

        verify(useCase).apply("e1", 1L, CourseVisibility.PRIVATE, CourseVisibility.PUBLIC)
    }

    @Test
    fun `null 공개범위는 그대로 통과시킨다`() {
        consumer.onMessage(
            CourseCountMessage(authorId = 2L, oldVisibility = null, newVisibility = null, eventId = "e2"),
        )

        verify(useCase).apply("e2", 2L, null, null)
    }

    @Test
    fun `미정의 공개범위는 예외를 던져 재처리하게 한다`() {
        assertThatThrownBy {
            consumer.onMessage(
                CourseCountMessage(authorId = 3L, oldVisibility = "PUBLIC", newVisibility = "UNKNOWN", eventId = "e3"),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
