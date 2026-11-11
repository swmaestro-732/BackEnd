package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.aichat.adapter.outbound.AiChatLocationAdapter.AreaCodeSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.opensearch.core.search.HitsMetadata
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.util.function.Function

/** [AiChatLocationAdapter] — 반경 질의 구성, 근처 장소 법정동코드 다수결, 엔진 부재·실패 시 null 을 검증한다. */
class AiChatLocationAdapterTest {
    private val here = Coordinate(37.5446, 127.0559)

    /** search 의 Function 오버로드를 고르기 위한 타입 지정 matcher. 제네릭 반환이라 null 검사가 끼지 않는다. */
    private fun <T> anyFn(): T = ArgumentMatchers.any()

    @Suppress("UNCHECKED_CAST")
    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    @Suppress("UNCHECKED_CAST")
    private fun clientReturning(areaCodes: List<String?>): OpenSearchClient {
        val client = mock(OpenSearchClient::class.java)
        val response = mock(SearchResponse::class.java) as SearchResponse<AreaCodeSource>
        val meta = mock(HitsMetadata::class.java) as HitsMetadata<AreaCodeSource>
        val hits =
            areaCodes.map { code ->
                val hit = mock(Hit::class.java) as Hit<AreaCodeSource>
                `when`(hit.source()).thenReturn(code?.let { AreaCodeSource(it) })
                hit
            }
        `when`(response.hits()).thenReturn(meta)
        `when`(meta.hits()).thenReturn(hits)
        `when`(
            client.search(
                anyFn<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>(),
                eq(AreaCodeSource::class.java),
            ),
        ).thenReturn(response)
        return client
    }

    private fun adapter(
        client: OpenSearchClient?,
        properties: OpenSearchProperties = OpenSearchProperties(),
    ) = AiChatLocationAdapter(providerOf(client), properties)

    @Test
    fun `클라이언트가 없으면 null`() {
        assertThat(adapter(null).findAreaCodeAt(here)).isNull()
    }

    @Test
    fun `검색이 실패하면 null 로 흡수한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(
            client.search(
                anyFn<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>(),
                eq(AreaCodeSource::class.java),
            ),
        ).thenThrow(RuntimeException("boom"))

        assertThat(adapter(client).findAreaCodeAt(here)).isNull()
    }

    @Test
    fun `근처 장소가 가진 법정동코드 중 가장 많은 값을 고르고 코드 없는 문서는 뺀다`() {
        val client = clientReturning(listOf("1120011400", null, "1120011500", "1120011400"))

        assertThat(adapter(client).findAreaCodeAt(here)).isEqualTo("1120011400")
    }

    @Test
    fun `근처에 장소가 없으면 null`() {
        assertThat(adapter(clientReturning(emptyList())).findAreaCodeAt(here)).isNull()
    }

    @Test
    fun `place alias 에서 areaCode 만 뽑아 1500m 안 20건을 거리순으로 조회한다`() {
        val client = clientReturning(listOf("1120011400"))

        adapter(client, OpenSearchProperties(indexPrefix = "dev-")).findAreaCodeAt(here)

        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(
                Function::class.java,
            ) as ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>
        verify(client).search(captor.capture(), eq(AreaCodeSource::class.java))
        val request = captor.value.apply(SearchRequest.Builder()).build()

        assertThat(request.index()).containsExactly("dev-place")
        assertThat(request.size()).isEqualTo(20)
        assertThat(request.source()!!.filter().includes()).containsExactly("areaCode")
        val filters = request.query()!!.bool().filter()
        assertThat(filters).hasSize(2)
        assertThat(filters[0].exists().field()).isEqualTo("areaCode")
        val geo = filters[1].geoDistance()
        assertThat(geo.field()).isEqualTo("location")
        assertThat(geo.distance()).isEqualTo("1500m")
        assertThat(geo.location().latlon().lat()).isEqualTo(37.5446)
        assertThat(geo.location().latlon().lon()).isEqualTo(127.0559)
        val sort = request.sort().single().geoDistance()
        assertThat(sort.field()).isEqualTo("location")
        assertThat(sort.order()).isEqualTo(SortOrder.Asc)
    }
}
