package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.event.CourseCounter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.json.JsonpSerializable
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorResponse
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch.core.UpdateRequest
import org.opensearch.client.opensearch.core.UpdateResponse
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.io.StringWriter
import java.util.function.Function

private typealias UpdateBuilder = UpdateRequest.Builder<CourseDocument, CourseDocument>
private typealias UpdateFn = Function<UpdateBuilder, ObjectBuilder<UpdateRequest<CourseDocument, CourseDocument>>>

/**
 * [OpenSearchCourseIndexAdapter.adjustCounter] 단위 테스트 — 카운터 델타 스크립트 요청 구성, delta 0·클라이언트 없음 no-op,
 * 미색인 문서(404)와 그 밖의 실패를 모두 fail-soft 로 삼키는지 검증한다.
 */
class OpenSearchCourseIndexAdapterCounterTest {
    private fun anyUpdateFn(): UpdateFn? = any()

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun clientCapturing(onRequest: (UpdateRequest<CourseDocument, CourseDocument>) -> Unit): OpenSearchClient {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.update(anyUpdateFn(), eq(CourseDocument::class.java))).thenAnswer { inv ->
            val builder = UpdateRequest.Builder<CourseDocument, CourseDocument>()
            inv.getArgument<UpdateFn>(0).apply(builder)
            onRequest(builder.build())
            @Suppress("UNCHECKED_CAST")
            mock(UpdateResponse::class.java) as UpdateResponse<CourseDocument>
        }
        return client
    }

    private fun toJson(value: JsonpSerializable): String {
        val mapper = JacksonJsonpMapper()
        val writer = StringWriter()
        mapper.jsonProvider().createGenerator(writer).use { generator -> value.serialize(generator, mapper) }
        return writer.toString()
    }

    private fun openSearchException(status: Int) =
        OpenSearchException(
            ErrorResponse.of { r -> r.status(status).error { e -> e.type("exception").reason("reason") } },
        )

    @Test
    fun `delta 가 0 이면 클라이언트를 부르지 않는다`() {
        val client = mock(OpenSearchClient::class.java)
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        adapter.adjustCounter(1L, CourseCounter.SAVES, 0)

        verify(client, never()).update(anyUpdateFn(), eq(CourseDocument::class.java))
    }

    @Test
    fun `클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(1L, CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }

    @Test
    fun `alias 필수·충돌 재시도·painless 델타 스크립트로 update 요청을 구성한다`() {
        var request: UpdateRequest<CourseDocument, CourseDocument>? = null
        val client = clientCapturing { request = it }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties(indexPrefix = "dev-"))

        adapter.adjustCounter(42L, CourseCounter.SAVES, -1)

        val built = request!!
        assertThat(built.index()).isEqualTo("dev-course")
        assertThat(built.requireAlias()).isTrue()
        assertThat(built.id()).isEqualTo("42")
        assertThat(built.retryOnConflict()).isEqualTo(3)
        val script = built.script()!!.inline()
        assertThat(script.lang()).isEqualTo("painless")
        assertThat(
            script.source(),
        ).contains("ctx._source[params.field]").contains("Math.max(0, current + (int) params.delta)")
        assertThat(toJson(built.script()!!)).contains("\"field\":\"savesCnt\"").contains("\"delta\":-1")
    }

    @Test
    fun `좋아요 카운터는 likesCnt 필드를 파라미터로 넘긴다`() {
        var request: UpdateRequest<CourseDocument, CourseDocument>? = null
        val client = clientCapturing { request = it }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        adapter.adjustCounter(7L, CourseCounter.LIKES, 1)

        assertThat(toJson(request!!.script()!!)).contains("\"field\":\"likesCnt\"").contains("\"delta\":1")
    }

    @Test
    fun `미색인 문서(404)는 정상 경로로 무시한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.update(anyUpdateFn(), eq(CourseDocument::class.java))).thenThrow(openSearchException(404))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(1L, CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }

    @Test
    fun `그 밖의 OpenSearch 오류도 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.update(anyUpdateFn(), eq(CourseDocument::class.java))).thenThrow(openSearchException(500))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(1L, CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }

    @Test
    fun `일반 예외도 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.update(anyUpdateFn(), eq(CourseDocument::class.java))).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client), OpenSearchProperties())

        assertThatCode { adapter.adjustCounter(1L, CourseCounter.SAVES, 1) }.doesNotThrowAnyException()
    }
}
