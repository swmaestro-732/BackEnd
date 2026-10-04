package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.adapter.messaging.CourseCountMessage
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class CourseCountSqsConsumerTest {
    private val useCase = mock(CourseCountUseCase::class.java)
    private val consumer = CourseCountSqsConsumer(useCase)

    @Test
    fun `유효한 공개범위 메시지를 수신하면 UseCase 를 파싱된 enum 으로 호출한다`() {
        consumer.onMessage(
            CourseCountMessage(
                authorId = 1L,
                oldVisibility = "PUBLIC",
                newVisibility = "PRIVATE",
                eventId = "evt-1",
            ),
        )

        verify(useCase).apply(
            eventId = "evt-1",
            authorId = 1L,
            oldVisibility = CourseVisibility.PUBLIC,
            newVisibility = CourseVisibility.PRIVATE,
        )
    }

    @Test
    fun `공개범위가 null 인 메시지는 UseCase 를 null 로 호출한다`() {
        consumer.onMessage(
            CourseCountMessage(
                authorId = 2L,
                oldVisibility = null,
                newVisibility = null,
                eventId = "evt-2",
            ),
        )

        verify(useCase).apply(
            eventId = "evt-2",
            authorId = 2L,
            oldVisibility = null,
            newVisibility = null,
        )
    }

    @Test
    fun `미정의 공개범위 수신 시 예외를 던져 ack 하지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            consumer.onMessage(
                CourseCountMessage(
                    authorId = 3L,
                    oldVisibility = "UNKNOWN_VISIBILITY",
                    newVisibility = null,
                    eventId = "evt-3",
                ),
            )
        }
    }
}
