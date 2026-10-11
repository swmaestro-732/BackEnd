package com.example.backend.user.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.geo.Coordinate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.json.JsonData
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.DistanceUnit
import org.opensearch.client.opensearch._types.GeoDistanceType
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.springframework.beans.factory.ObjectProvider

class OpenSearchSavedPlaceSearchAdapterTest {
    private val client = mock(OpenSearchClient::class.java)
    private val origin = Coordinate(37.544, 127.056)
    private val adapter =
        OpenSearchSavedPlaceSearchAdapter(providerOf(client), OpenSearchProperties(indexPrefix = "dev-"))

    @Test
    fun `클라이언트가 없으면 빈 목록을 반환한다`() {
        val noClient = OpenSearchSavedPlaceSearchAdapter(providerOf(null), OpenSearchProperties())

        assertThat(noClient.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `사용자와 반경을 검색 필터로 내려 거리순 size 개만 요청한다`() {
        stub(response(emptyList()))

        adapter.nearestSaved(7L, origin, 2010.0, 2)

        val request = capturedRequest()
        assertThat(request.index()).containsExactly("dev-saved_place")
        assertThat(request.size()).isEqualTo(2)
        assertThat(request.source()!!.filter().includes()).containsExactlyInAnyOrder("placeId", "visited")
        val filters = request.query()!!.bool().filter()
        assertThat(filters).hasSize(2)
        val userFilter = filters.single { it.isTerm }.term()
        assertThat(userFilter.field()).isEqualTo("userId")
        assertThat(userFilter.value().longValue()).isEqualTo(7L)
        val distanceFilter = filters.single { it.isGeoDistance }.geoDistance()
        assertThat(distanceFilter.field()).isEqualTo("location")
        assertThat(distanceFilter.distance()).isEqualTo("2010.0m")
        assertThat(distanceFilter.location().latlon().lat()).isEqualTo(origin.latitude)
        assertThat(distanceFilter.location().latlon().lon()).isEqualTo(origin.longitude)
        assertThat(request.sort()).hasSize(2)
        val distanceSort = request.sort().first().geoDistance()
        assertThat(distanceSort.field()).isEqualTo("location")
        assertThat(
            distanceSort
                .location()
                .single()
                .latlon()
                .lat(),
        ).isEqualTo(origin.latitude)
        assertThat(
            distanceSort
                .location()
                .single()
                .latlon()
                .lon(),
        ).isEqualTo(origin.longitude)
        assertThat(distanceSort.order()).isEqualTo(SortOrder.Asc)
        assertThat(distanceSort.distanceType()).isEqualTo(GeoDistanceType.Arc)
        assertThat(distanceSort.unit()).isEqualTo(DistanceUnit.Meters)
        assertThat(
            request
                .sort()
                .last()
                .doc()
                .order(),
        ).isEqualTo(SortOrder.Asc)
    }

    @Test
    fun `검색 순서를 유지하고 source 의 장소와 방문 상태 및 sort 의 미터 거리를 반환한다`() {
        stub(response(listOf(hit(20L, "152.7", true), hit(10L, "830.1", false))))

        val hits = adapter.nearestSaved(7L, origin, 2010.0, 2)

        assertThat(hits.map { it.placeId }).containsExactly(20L, 10L)
        assertThat(hits.map { it.distanceMeters }).containsExactly(152.7, 830.1)
        assertThat(hits.map { it.visited }).containsExactly(true, false)
    }

    @Test
    fun `반경 안 저장 장소가 없으면 빈 목록을 반환한다`() {
        stub(response(emptyList()))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `검색 타임아웃이면 부분 결과 대신 빈 목록을 반환한다`() {
        stub(response(listOf(hit(10L, "100.0", false)), timedOut = true))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `샤드가 실패하면 부분 결과 대신 빈 목록을 반환한다`() {
        stub(response(listOf(hit(10L, "100.0", false)), failedShards = 1))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `검색 예외는 빈 목록으로 흡수한다`() {
        `when`(client.search(any(SearchRequest::class.java), eq(JsonData::class.java)))
            .thenThrow(RuntimeException("search unavailable"))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `source 가 없는 문서도 예외를 전파하지 않는다`() {
        val malformed =
            Hit
                .Builder<JsonData>()
                .index("saved_place")
                .id("7:10")
                .sort(listOf("100.0"))
                .build()
        stub(response(listOf(malformed)))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    @Test
    fun `거리 정렬값이 잘못된 문서도 예외를 전파하지 않는다`() {
        stub(response(listOf(hit(10L, "invalid", false))))

        assertThat(adapter.nearestSaved(7L, origin, 2010.0, 2)).isEmpty()
    }

    private fun stub(response: SearchResponse<JsonData>) {
        `when`(client.search(any(SearchRequest::class.java), eq(JsonData::class.java))).thenReturn(response)
    }

    private fun capturedRequest(): SearchRequest {
        val captor = ArgumentCaptor.forClass(SearchRequest::class.java)
        verify(client).search(captor.capture(), eq(JsonData::class.java))
        return captor.value
    }

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun hit(
        placeId: Long,
        distance: String,
        visited: Boolean,
    ): Hit<JsonData> =
        Hit
            .Builder<JsonData>()
            .index("saved_place")
            .id("7:$placeId")
            .source(JsonData.of(mapOf("placeId" to placeId, "visited" to visited), JacksonJsonpMapper()))
            .sort(listOf(distance, "0"))
            .build()

    private fun response(
        hits: List<Hit<JsonData>>,
        timedOut: Boolean = false,
        failedShards: Int = 0,
    ): SearchResponse<JsonData> =
        SearchResponse
            .Builder<JsonData>()
            .took(1)
            .timedOut(timedOut)
            .shards { it.total(1).successful(1 - failedShards).failed(failedShards) }
            .hits { it.hits(hits) }
            .build()
}
