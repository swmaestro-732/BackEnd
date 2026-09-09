package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.common.domain.CourseVisibility
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider

/**
 * [CourseCountFallbackAdapter] 단위 테스트 — SqsTemplate 이 없을 때(로컬·CI) fail-soft no-op 을 검증한다.
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
            sqsTemplateProvider = emptyProvider,
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
}
