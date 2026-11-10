package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.event.CourseCounter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.json.JsonpSerializable
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorCause
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem
import org.opensearch.client.opensearch.core.bulk.OperationType
import org.opensearch.client.opensearch.core.bulk.UpdateOperation
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.io.StringWriter
import java.util.function.Function

private typealias BulkFn = Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>

/**
 * [OpenSearchCourseIndexAdapter.adjustCounter] 단위 테스트 — 코스 목록을 bulk 스크립트 갱신 한 요청으로 묶는지,
 * delta 0·빈 목록·클라이언트 없음 no-op, 항목별 404 와 그 밖의 실패·예외를 모두 fail-soft 로 삼키는지 검증한다.
 */
class OpenSearchCourseIndexAdapterCounterTest {
    private fun anyBulkFn(): BulkFn? = any()

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun bulkResponse(items: List<BulkResponseItem> = emptyList()): BulkResponse =
        BulkResponse.of { r -> r.errors(items.any { it.error() != null }).items(items).took(1L) }

    private fun item(
        id: String,
        status: Int,
        errorType: String? = null,
    ): BulkResponseItem =
        BulkResponseItem.of { b ->
            b
                .index("course")
                .id(id)
                .status(status)
                .operationType(OperationType.Update)
            errorType?.let { type -> b.error(ErrorCause.of { e -> e.type(type).reason("reason") }) }
            b
        }

    private fun clientCapturing(
        response: BulkResponse = bulkResponse(),
        onRequest: (BulkRequest) -> Unit,
    ): OpenSearchClient {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.bulk(anyBulkFn())).thenAnswer { inv ->
            val builder = BulkRequest.Builder()
            inv.getArgument<BulkFn>(0).apply(builder)
            onRequest(builder.build())
            response
        }
        return client
    }

    private fun toJson(value: JsonpSerializable): String {
        val mapper = JacksonJsonpMapper()
        val writer = StringWriter()
        mapper.jsonProvider().createGenerator(writer).use { generator -> value.serialize(generator, mapper) }
        return writer.toString()
    }

    /** bulk update 연산은 스크립트 접근자가 없어 NDJSON(메타 줄 + 본문 줄)으로 직렬화해 검사한다. */
    private fun ndjson(operation: UpdateOperation<CourseDocument>): String =
        operation
            ._serializables()
            .asSequence()
            .map { toJson(it as JsonpSerializable) }
            .joinToString("\n")

    @Test
    fun `delta 가 0 이거나 코스가 없으면 클라이언트를 부르지 않는다`() {
        val client = mock(OpenSearchClient::class.java)
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        adapter.adjustCounter(listOf(1L), CourseCounter.SAVES, 0)
        adapter.adjustCounter(emptyList(), CourseCounter.SAVES, 1)

        verify(client, never()).bulk(anyBulkFn())
    }

    @Test
    fun `클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(listOf(1L), CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }

    @Test
    fun `코스마다 alias 필수·충돌 재시도·painless 델타 스크립트 update 연산을 bulk 한 요청에 담는다`() {
        var request: BulkRequest? = null
        val client = clientCapturing { request = it }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties(indexPrefix = "dev-"))

        adapter.adjustCounter(listOf(42L, 43L, 42L), CourseCounter.SAVES, -1)

        val built = request!!
        assertThat(built.requireAlias()).isTrue()
        assertThat(built.operations()).hasSize(2) // 중복 id 는 한 번만
        val update = built.operations()[0].update<CourseDocument>()
        assertThat(update.index()).isEqualTo("dev-course")
        assertThat(update.id()).isEqualTo("42")
        assertThat(update.retryOnConflict()).isEqualTo(3)
        val json = ndjson(update)
        assertThat(json).contains("\"lang\":\"painless\"").contains("ctx._source[params.field]")
        assertThat(json).contains("Math.max(0, current + (int) params.delta)")
        assertThat(json).contains("\"field\":\"savesCnt\"").contains("\"delta\":-1")
        assertThat(built.operations()[1].update<CourseDocument>().id()).isEqualTo("43")
    }

    @Test
    fun `좋아요 카운터는 likesCnt 필드를 파라미터로 넘긴다`() {
        var request: BulkRequest? = null
        val client = clientCapturing { request = it }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        adapter.adjustCounter(listOf(7L), CourseCounter.LIKES, 1)

        assertThat(ndjson(request!!.operations()[0].update<CourseDocument>()))
            .contains("\"field\":\"likesCnt\"")
            .contains("\"delta\":1")
    }

    @Test
    fun `항목별 404 와 그 밖의 실패는 정상 경로로 삼킨다`() {
        val response =
            bulkResponse(
                listOf(
                    item("1", 200),
                    item("2", 404, errorType = "document_missing_exception"),
                    item("3", 409, errorType = "version_conflict_engine_exception"),
                ),
            )
        val client = clientCapturing(response) {}
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(listOf(1L, 2L, 3L), CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }

    @Test
    fun `클라이언트 예외도 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.bulk(anyBulkFn())).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(listOf(1L), CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }
}
