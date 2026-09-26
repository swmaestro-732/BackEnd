package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.domain.model.PlaceCategory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.aggregations.Aggregate
import org.opensearch.client.opensearch._types.aggregations.Buckets
import org.opensearch.client.opensearch._types.aggregations.GeoHashGridBucket
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.opensearch.core.search.HitsMetadata
import org.opensearch.client.opensearch.core.search.TotalHitsRelation
import org.springframework.beans.factory.ObjectProvider

/**
 * [OpenSearchPlaceSearchAdapter] 단위 테스트 — [OpenSearchClient] 를 목킹해 요청 DSL(필터·정렬·가산점)과
 * 응답 매핑(히트·격자 버킷 순서), 미가용·부분 실패 시 503 을 검증한다(Testcontainers 통합테스트와 별개, jacoco `test` 포함).
 */
class OpenSearchPlaceSearchAdapterTest {
    private val client: OpenSearchClient = mock(OpenSearchClient::class.java)
    private val adapter = OpenSearchPlaceSearchAdapter(providerOf(client), OpenSearchProperties(indexPrefix = "dev-"))

    @Test
    fun `클라이언트가 없으면 목록·지도 모두 503 이다`() {
        val noClient = OpenSearchPlaceSearchAdapter(providerOf(null), OpenSearchProperties())

        assertThatThrownBy { noClient.search(criteria()) }.isUnavailable()
        assertThatThrownBy { noClient.searchMap(criteria(viewport = viewport), 5, PlaceMapSort.RELEVANCE) }
            .isUnavailable()
    }

    @Test
    fun `목록 검색은 prefix alias 에 필터·가산점·정렬을 담아 요청하고 히트 id 와 전체 건수를 돌려준다`() {
        stub(response(hits = listOf(hit("3", "9.5"), hit("1", "2.0")), total = 42))

        val hits =
            adapter.search(
                criteria(
                    textTokens = listOf("블루보틀"),
                    categories = listOf(PlaceCategory.CAFE),
                    areaCodePrefixGroups = listOf(listOf("11"), listOf("11680", "11140")),
                    from = 10,
                    size = 5,
                    anchor = Coordinate(37.5, 127.0),
                    originalQuery = "블루보틀",
                ),
            )

        assertThat(hits.ids).containsExactly(3L, 1L)
        assertThat(hits.totalCount).isEqualTo(42L)
        val request = capturedRequest()
        assertThat(request.index()).containsExactly("dev-place")
        assertThat(request.from()).isEqualTo(10)
        assertThat(request.size()).isEqualTo(5)
        assertThat(request.sort().map { it.isScore to it.isDoc }).containsExactly(true to false, false to true)
        // 정확명 가산(term 필터 weight 1000) + 거리 가산(gauss) 두 함수를 관련도에 더한다.
        val functionScore = request.query()!!.functionScore()
        assertThat(functionScore.functions()).hasSize(2)
        assertThat(functionScore.functions()[0].weight()).isEqualTo(1000.0)
        assertThat(functionScore.functions()[1].isGauss).isTrue()
        // status + category + 지역 그룹 2개 = filter 4개, 텍스트는 must AND.
        val bool = functionScore.query()!!.bool()
        assertThat(bool.filter()).hasSize(4)
        assertThat(bool.filter()[1].isTerms).isTrue()
        assertThat(bool.filter()[2].bool().should()).hasSize(1)
        assertThat(bool.filter()[3].bool().should()).hasSize(2)
        assertThat(
            bool
                .must()
                .single()
                .multiMatch()
                .query(),
        ).isEqualTo("블루보틀")
    }

    @Test
    fun `필터만 있는 브라우즈는 가산점 없이 bool 질의와 _doc 정렬만 쓴다`() {
        stub(response(hits = emptyList(), total = 0))

        adapter.search(criteria(categories = listOf(PlaceCategory.CAFE)))

        val request = capturedRequest()
        assertThat(request.query()!!.isBool).isTrue()
        assertThat(request.sort().single().isDoc).isTrue()
        assertThat(request.query()!!.bool().must()).isEmpty()
    }

    @Test
    fun `목록 검색은 타임아웃·샤드 실패·예외를 모두 503 으로 바꾼다`() {
        stub(response(hits = emptyList(), total = 0, timedOut = true))
        assertThatThrownBy { adapter.search(criteria()) }.isUnavailable()

        stub(response(hits = emptyList(), total = 0, failedShards = 1))
        assertThatThrownBy { adapter.search(criteria()) }.isUnavailable()

        `when`(client.search(any(SearchRequest::class.java), eq(Void::class.java))).thenThrow(RuntimeException("down"))
        assertThatThrownBy { adapter.search(criteria()) }.isUnavailable()
    }

    @Test
    fun `지도 검색은 뷰포트 필터와 격자 집계를 요청하고 단일 셀을 점수순, 클러스터를 키순으로 돌려준다`() {
        stub(
            response(
                hits = listOf(hit("7", "1.0"), hit("3", "5.0")),
                total = 12,
                buckets =
                    listOf(
                        bucket("wydm9q", count = 1, sampleId = "7", sortValue = "1.0", lat = 37.1, lon = 127.1),
                        bucket("wydm9z", count = 10, sampleId = "9", sortValue = "3.0", lat = 37.3, lon = 127.3),
                        bucket("wydm9r", count = 1, sampleId = "3", sortValue = "5.0", lat = 37.2, lon = 127.2),
                    ),
            ),
        )

        val hits =
            adapter.searchMap(
                criteria(viewport = viewport, textTokens = listOf("카페")),
                6,
                PlaceMapSort.RELEVANCE,
            )

        assertThat(hits.totalCount).isEqualTo(12L)
        assertThat(hits.ids).containsExactly(7L, 3L)
        assertThat(hits.buckets.map { it.singlePlaceId }).containsExactly(3L, 7L, null)
        assertThat(hits.buckets.last().count).isEqualTo(10L)
        assertThat(hits.buckets.last().center).isEqualTo(Coordinate(37.3, 127.3))
        val request = capturedRequest()
        assertThat(request.index()).containsExactly("dev-place")
        assertThat(
            request
                .query()!!
                .bool()
                .filter()
                .any { it.isGeoBoundingBox },
        ).isTrue()
        assertThat(request.sort().first().isScore).isTrue()
        val grid = request.aggregations()["grid"]!!
        assertThat(grid.geohashGrid().precision()!!.geohashLength()).isEqualTo(6)
        assertThat(grid.aggregations().keys).containsExactlyInAnyOrder("center", "sample")
        assertThat(grid.aggregations()["sample"]!!.topHits().sort()).hasSize(2)
    }

    @Test
    fun `거리순은 기준점 geo_distance 정렬을 걸고 단일 셀을 거리 오름차순으로 돌려준다`() {
        stub(
            response(
                hits = listOf(hit("3", "120.0"), hit("7", "900.0")),
                total = 2,
                buckets =
                    listOf(
                        bucket("b", count = 1, sampleId = "7", sortValue = "900.0", lat = 37.9, lon = 127.9),
                        bucket("a", count = 1, sampleId = "3", sortValue = "120.0", lat = 37.5, lon = 127.0),
                    ),
            ),
        )

        val hits =
            adapter.searchMap(
                criteria(viewport = viewport, anchor = Coordinate(37.5, 127.0)),
                5,
                PlaceMapSort.DISTANCE,
            )

        assertThat(hits.buckets.map { it.singlePlaceId }).containsExactly(3L, 7L)
        val primary = capturedRequest().sort().first()
        assertThat(primary.isGeoDistance).isTrue()
        assertThat(
            primary
                .geoDistance()
                .location()
                .single()
                .latlon()
                .lat(),
        ).isEqualTo(37.5)
    }

    @Test
    fun `거리순에 기준점이 없으면 요청 전에 거절한다`() {
        assertThatThrownBy { adapter.searchMap(criteria(viewport = viewport), 5, PlaceMapSort.DISTANCE) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `격자 합이 전체 건수와 다르면 일부만 내려주지 않고 503 이다`() {
        stub(
            response(
                hits = listOf(hit("7", "1.0")),
                total = 300,
                buckets =
                    listOf(
                        bucket("wydm9q", count = 1, sampleId = "7", sortValue = "1.0", lat = 37.1, lon = 127.1),
                    ),
            ),
        )

        assertThatThrownBy {
            adapter.searchMap(
                criteria(viewport = viewport),
                5,
                PlaceMapSort.RELEVANCE,
            )
        }.isUnavailable()
    }

    private fun org.assertj.core.api.AbstractThrowableAssert<*, *>.isUnavailable() =
        isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE)

    private fun stub(response: SearchResponse<Void>) {
        `when`(client.search(any(SearchRequest::class.java), eq(Void::class.java))).thenReturn(response)
    }

    private fun capturedRequest(): SearchRequest {
        val captor = ArgumentCaptor.forClass(SearchRequest::class.java)
        org.mockito.Mockito
            .verify(
                client,
                org.mockito.Mockito.atLeastOnce(),
            ).search(captor.capture(), eq(Void::class.java))
        return captor.value
    }

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private val viewport = Viewport(Coordinate(37.0, 126.0), Coordinate(38.0, 128.0))

    private fun criteria(
        textTokens: List<String> = emptyList(),
        categories: List<PlaceCategory> = emptyList(),
        areaCodePrefixGroups: List<List<String>> = emptyList(),
        viewport: Viewport? = null,
        from: Int = 0,
        size: Int = 10,
        anchor: Coordinate? = null,
        originalQuery: String = "",
    ) = PlaceSearchCriteria(
        textTokens = textTokens,
        categories = categories,
        areaCodePrefixGroups = areaCodePrefixGroups,
        viewport = viewport,
        from = from,
        size = size,
        anchor = anchor,
        originalQuery = originalQuery,
    )

    private fun hit(
        id: String,
        sortValue: String,
    ): Hit<Void> =
        Hit
            .Builder<Void>()
            .index("place")
            .id(id)
            .sort(listOf(sortValue, "0"))
            .build()

    private fun bucket(
        key: String,
        count: Long,
        sampleId: String,
        sortValue: String,
        lat: Double,
        lon: Double,
    ): GeoHashGridBucket {
        val sample =
            Hit
                .Builder<JsonData>()
                .index("place")
                .id(sampleId)
                .sort(listOf(sortValue, "0"))
                .build()
        val sampleHits = HitsMetadata.Builder<JsonData>().hits(listOf(sample)).build()
        return GeoHashGridBucket
            .Builder()
            .key(key)
            .docCount(count)
            .aggregations(
                "center",
                Aggregate.of { a ->
                    a.geoCentroid { c ->
                        c.count(count).location { l -> l.latlon { ll -> ll.lat(lat).lon(lon) } }
                    }
                },
            ).aggregations("sample", Aggregate.of { a -> a.topHits { t -> t.hits(sampleHits) } })
            .build()
    }

    private fun response(
        hits: List<Hit<Void>>,
        total: Long,
        buckets: List<GeoHashGridBucket>? = null,
        timedOut: Boolean = false,
        failedShards: Int = 0,
    ): SearchResponse<Void> {
        val builder =
            SearchResponse
                .Builder<Void>()
                .took(1)
                .timedOut(timedOut)
                .shards { s -> s.total(1).successful(1 - failedShards).failed(failedShards) }
                .hits { h -> h.hits(hits).total { t -> t.value(total).relation(TotalHitsRelation.Eq) } }
        buckets?.let { list ->
            builder.aggregations(
                "grid",
                Aggregate.of { a ->
                    a.geohashGrid { g -> g.buckets(Buckets.of { b -> b.array(list) }) }
                },
            )
        }
        return builder.build()
    }
}
