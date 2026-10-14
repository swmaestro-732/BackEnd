package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorCause
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.DeleteRequest
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant
import java.util.function.Function

class OpenSearchSavedPlaceIndexAdapterTest {
    @Suppress("UNCHECKED_CAST")
    private val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val client = mock(OpenSearchClient::class.java)
    private val adapter = OpenSearchSavedPlaceIndexAdapter(provider, OpenSearchProperties(indexPrefix = "dev-"))

    @Test
    fun `빈 문서 목록은 클라이언트를 조회하지 않는다`() {
        adapter.index(emptyList())

        verify(provider, never()).ifAvailable
    }

    @Test
    fun `클라이언트가 없으면 단건과 배치 색인 및 삭제는 no-op 이다`() {
        `when`(provider.ifAvailable).thenReturn(null)

        assertThatCode {
            adapter.index(document())
            adapter.index(listOf(document()))
            adapter.delete(7L, 10L)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `단건 색인은 prefix alias 와 복합 문서 ID 및 좌표 방문 상태 저장 시각을 보낸다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        var request: BulkRequest? = null
        captureBulk { request = it }
        val saved = document()

        adapter.index(saved)

        val bulk = requireNotNull(request)
        assertThat(bulk.requireAlias()).isTrue()
        assertThat(bulk.operations()).hasSize(1)
        val operation = bulk.operations().single().index<Map<String, Any>>()
        assertThat(operation.index()).isEqualTo("dev-saved_place")
        assertThat(operation.id()).isEqualTo("7:10")
        val payload = operation.document()
        assertThat(payload["userId"]).isEqualTo(7L)
        assertThat(payload["placeId"]).isEqualTo(10L)
        assertThat(payload["location"]).isEqualTo(mapOf("lat" to 37.544, "lon" to 127.056))
        assertThat(payload).doesNotContainKey("visited")
        assertThat(payload["savedAt"]).isEqualTo(saved.savedAt.toEpochMilli())
    }

    @Test
    fun `배치 색인은 각 사용자 장소 쌍을 독립적인 upsert 로 보낸다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        var request: BulkRequest? = null
        captureBulk { request = it }

        adapter.index(listOf(document(), document(userId = 8L), document(placeId = 20L)))

        val bulk = requireNotNull(request)
        assertThat(bulk.requireAlias()).isTrue()
        assertThat(bulk.operations().map { it.index<Any>().id() }).containsExactly("7:10", "8:10", "7:20")
        assertThat(bulk.operations().map { it.index<Any>().index() }).containsOnly("dev-saved_place")
    }

    @Test
    fun `단건 색인 전송 실패는 전파한다 (리스너가 폴백 큐로 재처리)`() {
        `when`(provider.ifAvailable).thenReturn(client)
        doAnswer { throw RuntimeException("index unavailable") }
            .`when`(client)
            .bulk(anyArg<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>())

        assertThatThrownBy { adapter.index(document()) }.isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `배치 색인 전송 실패는 전파하지 않는다 (재색인 best-effort)`() {
        `when`(provider.ifAvailable).thenReturn(client)
        doAnswer { throw RuntimeException("index unavailable") }
            .`when`(client)
            .bulk(anyArg<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>())

        assertThatCode { adapter.index(listOf(document())) }.doesNotThrowAnyException()
    }

    @Test
    fun `bulk 부분 실패는 확인하되 예외를 전파하지 않는다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        val successful = mock(BulkResponseItem::class.java)
        val failed = mock(BulkResponseItem::class.java)
        `when`(failed.error()).thenReturn(mock(ErrorCause::class.java))
        val response = mock(BulkResponse::class.java)
        `when`(response.errors()).thenReturn(true)
        `when`(response.items()).thenReturn(listOf(successful, failed))
        doAnswer { response }
            .`when`(client)
            .bulk(anyArg<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>())

        assertThatCode { adapter.index(listOf(document(), document(placeId = 20L))) }.doesNotThrowAnyException()
        verify(response).items()
        verify(successful).error()
        verify(failed).error()
    }

    @Test
    fun `삭제는 prefix alias 에서 해당 사용자 장소 복합 ID 만 제거한다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        var request: DeleteRequest? = null
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val fn = invocation.arguments[0] as Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>
            request = fn.apply(DeleteRequest.Builder()).build()
            null
        }.`when`(client).delete(anyArg<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        adapter.delete(7L, 10L)

        assertThat(requireNotNull(request).index()).isEqualTo("dev-saved_place")
        assertThat(requireNotNull(request).id()).isEqualTo("7:10")
    }

    @Test
    fun `삭제 전송 실패는 전파한다 (리스너가 폴백 큐로 재처리)`() {
        `when`(provider.ifAvailable).thenReturn(client)
        doAnswer { throw RuntimeException("delete unavailable") }
            .`when`(client)
            .delete(anyArg<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        assertThatThrownBy { adapter.delete(7L, 10L) }.isInstanceOf(RuntimeException::class.java)
    }

    private fun captureBulk(capture: (BulkRequest) -> Unit) {
        val response = mock(BulkResponse::class.java)
        `when`(response.errors()).thenReturn(false)
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val fn = invocation.arguments[0] as Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>
            capture(fn.apply(BulkRequest.Builder()).build())
            response
        }.`when`(client).bulk(anyArg<Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>())
    }

    private fun document(
        userId: Long = 7L,
        placeId: Long = 10L,
    ) = SavedPlaceIndexDocument(
        userId = userId,
        placeId = placeId,
        location = Coordinate(37.544, 127.056),
        savedAt = Instant.parse("2026-10-11T08:00:00Z"),
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T> anyArg(): T = org.mockito.Mockito.any<T>() as T
