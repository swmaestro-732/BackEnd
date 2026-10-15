package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import io.awspring.cloud.sqs.operations.SqsTemplate
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant

/**
 * [SavedPlaceIndexFallbackAdapter] 단위 테스트 — SqsTemplate 유무·queueUrl 설정에 따른 발행/no-op 를 검증한다.
 * SQS 실전송은 통합 환경에서 다룬다(CourseCountFallbackAdapterTest 선례).
 */
class SavedPlaceIndexFallbackAdapterTest {
    private val document =
        SavedPlaceIndexDocument(
            userId = 7L,
            placeId = 42L,
            location = Coordinate(37.51, 127.02),
            savedAt = Instant.parse("2026-10-11T08:00:00Z"),
            name = "어니언 성수",
            category = "CAFE",
            imageUrl = "https://img/42.jpg",
            rating = 4.8,
            ratingCount = 212,
        )

    private val emptyProvider =
        object : ObjectProvider<SqsTemplate> {
            override fun getObject(vararg args: Any?): SqsTemplate = error("no template")

            override fun getObject(): SqsTemplate = error("no template")

            override fun getIfAvailable(): SqsTemplate? = null

            override fun getIfUnique(): SqsTemplate? = null

            override fun iterator(): MutableIterator<SqsTemplate> = mutableListOf<SqsTemplate>().iterator()
        }

    @Test
    fun `SqsTemplate 이 없으면 예외 없이 no-op 한다`() {
        val adapter = SavedPlaceIndexFallbackAdapter(emptyProvider, SqsProperties(savedPlaceQueueUrl = ""))
        assertDoesNotThrow {
            adapter.enqueueIndex(document)
            adapter.enqueueDelete(7L, 42L)
        }
    }

    @Test
    fun `SqsTemplate 이 있고 queueUrl 이 설정됐으면 send 를 호출한다`() {
        val template = mock(SqsTemplate::class.java)
        val adapter =
            SavedPlaceIndexFallbackAdapter(
                providerOf(template),
                SqsProperties(savedPlaceQueueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123/saved-place"),
            )

        adapter.enqueueIndex(document)

        val sendCalled = Mockito.mockingDetails(template).invocations.any { it.method.name == "send" }
        assertTrue(sendCalled, "SqsTemplate.send 가 호출돼야 한다")
    }

    @Test
    fun `queueUrl 이 비면 send 를 호출하지 않는다`() {
        val template = mock(SqsTemplate::class.java)
        val adapter = SavedPlaceIndexFallbackAdapter(providerOf(template), SqsProperties(savedPlaceQueueUrl = ""))

        adapter.enqueueDelete(7L, 42L)

        val sendCalled = Mockito.mockingDetails(template).invocations.any { it.method.name == "send" }
        assertTrue(!sendCalled, "queueUrl 이 비면 send 가 호출돼서는 안 된다")
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
