package com.example.backend.course.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.course.application.event.CourseCountDeltaEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import software.amazon.awssdk.services.sqs.SqsClient
import tools.jackson.databind.json.JsonMapper

/**
 * [SqsCourseCountPublishAdapter] 단위 테스트 — 클라이언트가 없을 때(로컬·CI) fail-soft no-op 을 검증한다.
 * SQS 실전송은 통합 환경에서 다룬다.
 */
class SqsCourseCountPublishAdapterTest {
    /** SqsClient 빈이 없는 상황(큐 미설정) — getIfAvailable 이 null 을 준다. */
    private val emptyProvider =
        object : ObjectProvider<SqsClient> {
            override fun getObject(vararg args: Any?): SqsClient = error("no client")

            override fun getObject(): SqsClient = error("no client")

            override fun getIfAvailable(): SqsClient? = null

            override fun getIfUnique(): SqsClient? = null

            override fun iterator(): MutableIterator<SqsClient> = mutableListOf<SqsClient>().iterator()
        }

    private val adapter =
        SqsCourseCountPublishAdapter(
            clientProvider = emptyProvider,
            sqsProperties = SqsProperties(courseCountQueueUrl = ""),
            objectMapper = JsonMapper.builder().build(),
        )

    @Test
    fun `클라이언트가 없으면 예외 없이 no-op 한다`() {
        val event =
            CourseCountDeltaEvent(
                userId = 1L,
                publicDelta = 1,
                followerDelta = 0,
                privateDelta = 0,
                eventId = "e1",
            )

        assertDoesNotThrow { adapter.send(event) }
    }
}
