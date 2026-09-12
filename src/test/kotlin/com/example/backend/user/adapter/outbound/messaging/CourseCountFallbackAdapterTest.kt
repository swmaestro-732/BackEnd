package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.adapter.messaging.CourseCountMessage
import io.awspring.cloud.sqs.operations.SqsSendOptions
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import java.util.function.Consumer

/**
 * [CourseCountFallbackAdapter] 단위 테스트 — SqsTemplate 미설정 시 no-op, 설정 시 올바른 [CourseCountMessage] 발행,
 * queueUrl 미설정·전송 예외의 fail-soft 를 검증한다.
 */
class CourseCountFallbackAdapterTest {
    private fun providerOf(template: SqsTemplate?): ObjectProvider<SqsTemplate> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<SqsTemplate>
        `when`(provider.ifAvailable).thenReturn(template)
        return provider
    }

    @Test
    fun `SqsTemplate 이 없으면 예외 없이 no-op 한다`() {
        val adapter = CourseCountFallbackAdapter(providerOf(null), SqsProperties(courseCountQueueUrl = ""))

        assertDoesNotThrow {
            adapter.publish(
                authorId = 1L,
                oldVisibility = null,
                newVisibility = CourseVisibility.PUBLIC,
                eventId = "e1",
            )
        }
    }

    @Test
    fun `queueUrl 이 비면 발행하지 않는다`() {
        val template = mock(SqsTemplate::class.java)
        val adapter = CourseCountFallbackAdapter(providerOf(template), SqsProperties(courseCountQueueUrl = ""))

        adapter.publish(authorId = 1L, oldVisibility = null, newVisibility = CourseVisibility.PUBLIC, eventId = "e1")

        verify(template, never()).send(any<Consumer<SqsSendOptions<Any>>>())
    }

    @Test
    fun `SqsTemplate 이 있으면 올바른 메시지로 발행한다`() {
        val template = mock(SqsTemplate::class.java)
        val adapter =
            CourseCountFallbackAdapter(providerOf(template), SqsProperties(courseCountQueueUrl = "https://q/fallback"))

        adapter.publish(
            authorId = 42L,
            oldVisibility = CourseVisibility.PUBLIC,
            newVisibility = CourseVisibility.PRIVATE,
            eventId = "evt-1",
        )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Consumer::class.java) as ArgumentCaptor<Consumer<SqsSendOptions<Any>>>
        verify(template).send(captor.capture())

        @Suppress("UNCHECKED_CAST")
        val options = mock(SqsSendOptions::class.java) as SqsSendOptions<Any>
        `when`(options.queue(any())).thenReturn(options)
        `when`(options.payload(any())).thenReturn(options)
        captor.value.accept(options)

        verify(options).queue("https://q/fallback")
        val msgCaptor = ArgumentCaptor.forClass(CourseCountMessage::class.java)
        verify(options).payload(msgCaptor.capture())
        val msg = msgCaptor.value
        assertThat(msg.authorId).isEqualTo(42L)
        assertThat(msg.oldVisibility).isEqualTo("PUBLIC")
        assertThat(msg.newVisibility).isEqualTo("PRIVATE")
        assertThat(msg.eventId).isEqualTo("evt-1")
    }

    @Test
    fun `null 공개범위는 메시지에서도 null 로 실린다`() {
        val template = mock(SqsTemplate::class.java)
        val adapter =
            CourseCountFallbackAdapter(providerOf(template), SqsProperties(courseCountQueueUrl = "https://q/fallback"))

        adapter.publish(authorId = 1L, oldVisibility = null, newVisibility = null, eventId = "e2")

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Consumer::class.java) as ArgumentCaptor<Consumer<SqsSendOptions<Any>>>
        verify(template).send(captor.capture())

        @Suppress("UNCHECKED_CAST")
        val options = mock(SqsSendOptions::class.java) as SqsSendOptions<Any>
        `when`(options.queue(any())).thenReturn(options)
        `when`(options.payload(any())).thenReturn(options)
        captor.value.accept(options)

        val msgCaptor = ArgumentCaptor.forClass(CourseCountMessage::class.java)
        verify(options).payload(msgCaptor.capture())
        assertThat(msgCaptor.value.oldVisibility).isNull()
        assertThat(msgCaptor.value.newVisibility).isNull()
    }

    @Test
    fun `발행 중 예외가 나도 fail-soft 로 삼킨다`() {
        val template = mock(SqsTemplate::class.java)
        `when`(template.send(any<Consumer<SqsSendOptions<Any>>>())).thenThrow(RuntimeException("boom"))
        val adapter =
            CourseCountFallbackAdapter(providerOf(template), SqsProperties(courseCountQueueUrl = "https://q/fallback"))

        assertThatCode {
            adapter.publish(
                authorId = 1L,
                oldVisibility = null,
                newVisibility = CourseVisibility.PUBLIC,
                eventId = "e3",
            )
        }.doesNotThrowAnyException()
    }
}
