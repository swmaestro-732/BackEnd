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
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.DeleteRequest
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.IndexResponse
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
            adapter.delete(7L, 10L, 1000L)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `단건 색인은 prefix alias 와 복합 ID, external version(savedAt), 좌표·표시정보를 보낸다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        var request: IndexRequest<*>? = null
        captureIndex { request = it }
        val saved = document()

        adapter.index(saved)

        val req = requireNotNull(request)
        assertThat(req.index()).isEqualTo("dev-saved_place")
        assertThat(req.id()).isEqualTo("7:10")
        assertThat(req.requireAlias()).isTrue()
        assertThat(req.versionType()).isEqualTo(VersionType.External)
        assertThat(req.version()).isEqualTo(saved.savedAt.toEpochMilli())
        @Suppress("UNCHECKED_CAST")
        val payload = req.document() as Map<String, Any?>
        assertThat(payload["userId"]).isEqualTo(7L)
        assertThat(payload["placeId"]).isEqualTo(10L)
        assertThat(payload["location"]).isEqualTo(mapOf("lat" to 37.544, "lon" to 127.056))
        assertThat(payload["name"]).isEqualTo("어니언 성수")
        assertThat(payload["rating"]).isEqualTo(4.8)
        assertThat(payload).doesNotContainKey("visited")
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
            .index(anyArg<Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>>())

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
    fun `배치 부분 실패는 확인하되 예외를 전파하지 않는다`() {
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
    fun `삭제는 prefix alias 에서 복합 ID 를 external version(removedAt)으로 제거한다`() {
        `when`(provider.ifAvailable).thenReturn(client)
        var request: DeleteRequest? = null
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val fn = invocation.arguments[0] as Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>
            request = fn.apply(DeleteRequest.Builder()).build()
            null
        }.`when`(client).delete(anyArg<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        adapter.delete(7L, 10L, 5000L)

        val req = requireNotNull(request)
        assertThat(req.index()).isEqualTo("dev-saved_place")
        assertThat(req.id()).isEqualTo("7:10")
        assertThat(req.versionType()).isEqualTo(VersionType.External)
        assertThat(req.version()).isEqualTo(5000L)
    }

    @Test
    fun `삭제 전송 실패는 전파한다 (리스너가 폴백 큐로 재처리)`() {
        `when`(provider.ifAvailable).thenReturn(client)
        doAnswer { throw RuntimeException("delete unavailable") }
            .`when`(client)
            .delete(anyArg<Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        assertThatThrownBy { adapter.delete(7L, 10L, 5000L) }.isInstanceOf(RuntimeException::class.java)
    }

    private fun captureIndex(capture: (IndexRequest<*>) -> Unit) {
        doAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val fn = invocation.arguments[0] as Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>
            capture(fn.apply(IndexRequest.Builder()).build())
            mock(IndexResponse::class.java)
        }.`when`(client).index(anyArg<Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>>())
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
        name = "어니언 성수",
        category = "CAFE",
        imageUrl = "https://img/$placeId.jpg",
        rating = 4.8,
        ratingCount = 212,
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T> anyArg(): T = org.mockito.Mockito.any<T>() as T
