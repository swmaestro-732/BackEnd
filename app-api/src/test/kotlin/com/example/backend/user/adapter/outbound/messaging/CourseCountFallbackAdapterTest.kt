package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.sqs.SqsMessageSender
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.ObjectProvider

/**
 * [CourseCountFallbackAdapter] 단위 테스트. SqsTemplate 이 없을 때(로컬, CI) no-op, 전송 실패 시 예외 전파를 검증한다.
 * SQS 실전송은 통합 환경에서 다룬다.
 */
class CourseCountFallbackAdapterTest {
    /** SqsTemplate 빈이 없는 상황(SQS 미설정) — getIfAvailable 이 null 을 준다. */
    private val emptyProvider =
        object : ObjectProvider<SqsTemplate> {
            override fun getObject(vararg args: Any?): SqsTemplate = error("no template")

            override fun getObject(): SqsTemplate = error("no template")

            override fun getIfAvailable(): SqsTemplate? = null

            override fun getIfUnique(): SqsTemplate? = null

            override fun iterator(): MutableIterator<SqsTemplate> = mutableListOf<SqsTemplate>().iterator()
        }

    private val adapter =
        CourseCountFallbackAdapter(
            sqsMessageSender = SqsMessageSender(emptyProvider),
            sqsProperties = SqsProperties(courseCountQueueUrl = ""),
        )

    @Test
    fun `SqsTemplate 이 없으면 예외 없이 no-op 한다`() {
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
    fun `SqsTemplate 이 있고 queueUrl 이 설정됐으면 send 를 호출한다`() {
        val mockTemplate = mock(SqsTemplate::class.java)
        val adapterWithTemplate =
            CourseCountFallbackAdapter(
                sqsMessageSender = SqsMessageSender(providerOf(mockTemplate)),
                sqsProperties =
                    SqsProperties(
                        courseCountQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/test-queue",
                    ),
            )

        adapterWithTemplate.publish(
            authorId = 10L,
            oldVisibility = CourseVisibility.PUBLIC,
            newVisibility = CourseVisibility.PRIVATE,
            eventId = "evt-send",
        )

        // SqsTemplate.send 는 제네릭 메서드라 Mockito verify 호출에 타입 추론 제약이 있다.
        // mockingDetails 로 send 가 실제 호출됐는지 확인한다.
        val sendCalled =
            Mockito
                .mockingDetails(mockTemplate)
                .invocations
                .any { it.method.name == "send" }
        assertTrue(sendCalled, "SqsTemplate.send 가 호출돼야 한다")
    }

    @Test
    fun `SqsTemplate 이 있어도 queueUrl 이 비면 send 를 호출하지 않는다`() {
        val mockTemplate = mock(SqsTemplate::class.java)
        val adapterBlankUrl =
            CourseCountFallbackAdapter(
                sqsMessageSender = SqsMessageSender(providerOf(mockTemplate)),
                sqsProperties = SqsProperties(courseCountQueueUrl = ""),
            )

        adapterBlankUrl.publish(
            authorId = 10L,
            oldVisibility = null,
            newVisibility = CourseVisibility.PUBLIC,
            eventId = "evt-blank",
        )

        val sendCalled =
            Mockito
                .mockingDetails(mockTemplate)
                .invocations
                .any { it.method.name == "send" }
        assertTrue(!sendCalled, "queueUrl 이 비면 send 가 호출돼서는 안 된다")
    }

    @Test
    fun `SQS 전송이 실패하면 예외를 던져 이벤트 재발행에 맡긴다`() {
        val failingTemplate = mock(SqsTemplate::class.java) { throw IllegalStateException("SQS 장애") }
        val adapterWithFailingTemplate =
            CourseCountFallbackAdapter(
                sqsMessageSender = SqsMessageSender(providerOf(failingTemplate)),
                sqsProperties =
                    SqsProperties(
                        courseCountQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/test-queue",
                    ),
            )

        assertThrows(IllegalStateException::class.java) {
            adapterWithFailingTemplate.publish(
                authorId = 10L,
                oldVisibility = null,
                newVisibility = CourseVisibility.PUBLIC,
                eventId = "evt-fail",
            )
        }
    }

    private fun providerOf(template: SqsTemplate): ObjectProvider<SqsTemplate> =
        object : ObjectProvider<SqsTemplate> {
            override fun getObject(vararg args: Any?): SqsTemplate = template

            override fun getObject(): SqsTemplate = template

            override fun getIfAvailable(): SqsTemplate = template

            override fun getIfUnique(): SqsTemplate = template

            override fun iterator(): MutableIterator<SqsTemplate> = mutableListOf(template).iterator()
        }
}
