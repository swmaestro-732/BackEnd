package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.CourseRecommendProperties
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.port.outbound.CourseRecommendAfter
import com.example.backend.course.application.port.outbound.CourseRecommendCriteria
import com.example.backend.course.application.port.outbound.CourseRegionFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.FieldValueFactorModifier
import org.opensearch.client.opensearch._types.query_dsl.FunctionBoostMode
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreMode
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.opensearch.core.search.HitsMetadata
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant

/**
 * [OpenSearchCourseRecommendAdapter] 단위 테스트 — [OpenSearchClient] 를 목킹해 function_score 요청 구성(필터·관심 should·
 * 감쇠/저장수 함수·정렬·search_after)과 응답 매핑·hasNext 판정·fail-soft(null) 를 검증한다.
 */
class OpenSearchCourseRecommendAdapterTest {
    private val origin = Instant.parse("2026-11-03T00:00:00Z")
    private val mapper = JacksonJsonpMapper()

    private val weights =
        CourseRecommendProperties(
            recencyWeight = 7.0,
            recencyScale = "10d",
            recencyOffset = "2d",
            recencyDecay = 0.4,
            themeWeight = 4.5,
            sigunguWeight = 3.5,
            dongWeight = 2.5,
            savesWeight = 1.25,
            likesWeight = 0.5,
        )

    private fun doc(
        id: Long,
        savesCnt: Int = 11,
    ) = CourseDocument(
        id = id,
        title = "코스 $id",
        description = "설명",
        area = "성수",
        category = listOf("CAFETOUR"),
        tags = listOf("데이트"),
        coverImageUrl = "https://img/$id.jpg",
        visibility = "PUBLIC",
        isPublished = true,
        userId = "7",
        likesCnt = 3,
        savesCnt = savesCnt,
        createdAt = 1000L,
    )

    @Suppress("UNCHECKED_CAST")
    private fun hit(
        doc: CourseDocument?,
        score: Double? = 1.5,
    ): Hit<CourseDocument> {
        val h = mock(Hit::class.java) as Hit<CourseDocument>
        `when`(h.source()).thenReturn(doc)
        `when`(h.score()).thenReturn(score)
        return h
    }

    @Suppress("UNCHECKED_CAST")
    private fun clientReturning(hits: List<Hit<CourseDocument>>): OpenSearchClient {
        val client = mock(OpenSearchClient::class.java)
        val response = mock(SearchResponse::class.java) as SearchResponse<CourseDocument>
        val meta = mock(HitsMetadata::class.java) as HitsMetadata<CourseDocument>
        `when`(response.hits()).thenReturn(meta)
        `when`(meta.hits()).thenReturn(hits)
        `when`(client.search(any(SearchRequest::class.java), eq(CourseDocument::class.java))).thenReturn(response)
        return client
    }

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun adapter(
        client: OpenSearchClient?,
        properties: OpenSearchProperties = OpenSearchProperties(),
    ) = OpenSearchCourseRecommendAdapter(providerOf(client), properties, weights)

    private fun criteria(
        likeThemes: List<String> = listOf("CAFETOUR", "FOOD"),
        sigunguCodes: List<String> = listOf("11680"),
        dongCodes: List<String> = listOf("1168010100"),
        category: String? = null,
        region: CourseRegionFilter? = null,
        after: CourseRecommendAfter? = null,
        size: Int = 2,
        placeId: Long? = null,
        excludeIds: List<Long> = emptyList(),
        keyword: String? = null,
    ) = CourseRecommendCriteria(
        likeThemes = likeThemes,
        sigunguCodes = sigunguCodes,
        dongCodes = dongCodes,
        category = category,
        region = region,
        origin = origin,
        after = after,
        size = size,
        placeId = placeId,
        excludeIds = excludeIds,
        keyword = keyword,
    )

    private fun capturedRequest(
        client: OpenSearchClient,
        criteria: CourseRecommendCriteria,
        properties: OpenSearchProperties = OpenSearchProperties(),
    ): SearchRequest {
        adapter(client, properties).recommend(criteria)
        val captor = ArgumentCaptor.forClass(SearchRequest::class.java)
        verify(client).search(captor.capture(), eq(CourseDocument::class.java))
        return captor.value
    }

    @Test
    fun `클라이언트가 없으면 null 로 DB 폴백을 알린다`() {
        assertThat(adapter(null).recommend(criteria())).isNull()
    }

    @Test
    fun `검색 중 예외가 나면 null 로 흡수한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.search(any(SearchRequest::class.java), eq(CourseDocument::class.java)))
            .thenThrow(RuntimeException("boom"))

        assertThat(adapter(client).recommend(criteria())).isNull()
    }

    @Test
    fun `size 초과 히트가 있으면 hasNext true 로 잘라내고 행과 점수를 매핑한다`() {
        val client = clientReturning(listOf(hit(doc(1), 9.5), hit(doc(2), 8.25), hit(doc(3), 7.0)))

        val page = adapter(client).recommend(criteria(size = 2))!!

        assertThat(page.hasNext).isTrue()
        assertThat(page.hits.map { it.row.id }).containsExactly(1L, 2L)
        assertThat(page.hits.map { it.score }).containsExactly(9.5, 8.25)
        val row = page.hits.first().row
        assertThat(row.authorId).isEqualTo(7L)
        assertThat(row.title).isEqualTo("코스 1")
        assertThat(row.themes).containsExactly("CAFETOUR")
        assertThat(row.area).isEqualTo("성수")
        assertThat(row.savesCnt).isEqualTo(11)
        assertThat(row.createdAt).isEqualTo(Instant.ofEpochMilli(1000L))
    }

    @Test
    fun `다음 페이지가 없으면 hasNext false 이고 source 없는 히트는 건너뛰며 점수 없으면 0`() {
        val client = clientReturning(listOf(hit(doc(1), null), hit(null, 3.0)))

        val page = adapter(client).recommend(criteria(size = 2))!!

        assertThat(page.hasNext).isFalse()
        assertThat(page.hits).hasSize(1)
        assertThat(
            page.hits
                .single()
                .row.id,
        ).isEqualTo(1L)
        assertThat(page.hits.single().score).isEqualTo(0.0)
    }

    @Test
    fun `환경 접두사가 붙은 course alias 를 size 초과 1건으로 조회한다`() {
        val request =
            capturedRequest(
                clientReturning(emptyList()),
                criteria(size = 5),
                OpenSearchProperties(indexPrefix = "dev-"),
            )

        assertThat(request.index()).containsExactly("dev-course")
        assertThat(request.size()).isEqualTo(6)
    }

    @Test
    fun `function_score 는 최신성 exp 감쇠와 저장수 log1p 를 가중치와 함께 sum 으로 더한다`() {
        val request = capturedRequest(clientReturning(emptyList()), criteria())

        val fs = request.query()!!.functionScore()
        assertThat(fs.scoreMode()).isEqualTo(FunctionScoreMode.Sum)
        assertThat(fs.boostMode()).isEqualTo(FunctionBoostMode.Sum)
        assertThat(fs.functions()).hasSize(3)

        val recency = fs.functions()[0]
        assertThat(recency.weight()).isEqualTo(7.0)
        assertThat(recency.exp().field()).isEqualTo("createdAt")
        val placement = recency.exp().placement()
        assertThat(placement.origin()!!.to(Long::class.java, mapper)).isEqualTo(origin.toEpochMilli())
        assertThat(placement.scale()!!.to(String::class.java, mapper)).isEqualTo("10d")
        assertThat(placement.offset()!!.to(String::class.java, mapper)).isEqualTo("2d")
        assertThat(placement.decay()).isEqualTo(0.4)

        val saves = fs.functions()[1]
        assertThat(saves.weight()).isEqualTo(1.25)
        assertThat(saves.fieldValueFactor().field()).isEqualTo("savesCnt")
        assertThat(saves.fieldValueFactor().modifier()).isEqualTo(FieldValueFactorModifier.Log1p)
        assertThat(saves.fieldValueFactor().missing()).isEqualTo(0.0)
        val likes = fs.functions()[2]
        assertThat(likes.weight()).isEqualTo(0.5)
        assertThat(likes.fieldValueFactor().field()).isEqualTo("likesCnt")
        assertThat(likes.fieldValueFactor().modifier()).isEqualTo(FieldValueFactorModifier.Log1p)
        assertThat(likes.fieldValueFactor().missing()).isEqualTo(0.0)
    }

    @Test
    fun `base 필터는 발행 PUBLIC 과 id 존재이며 카테고리는 선택 필터로 붙는다`() {
        val withoutCategory = capturedRequest(clientReturning(emptyList()), criteria())
        val withCategory = capturedRequest(clientReturning(emptyList()), criteria(category = "FOOD"))

        val filters =
            withoutCategory
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .filter()
        assertThat(filters).hasSize(3)
        assertThat(filters[0].term().field()).isEqualTo("visibility")
        assertThat(filters[0].term().value().stringValue()).isEqualTo("PUBLIC")
        assertThat(filters[1].term().field()).isEqualTo("isPublished")
        assertThat(filters[1].term().value().booleanValue()).isTrue()
        assertThat(filters[2].exists().field()).isEqualTo("id")

        val categoryFilters =
            withCategory
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .filter()
        assertThat(categoryFilters).hasSize(4)
        assertThat(categoryFilters[3].term().field()).isEqualTo("category")
        assertThat(categoryFilters[3].term().value().stringValue()).isEqualTo("FOOD")
    }

    @Test
    fun `region 필터는 include prefix 를 should(최소 1개) 로, exclude prefix 를 must_not 으로 붙인다`() {
        val region = CourseRegionFilter(includePrefixes = listOf("12"), excludePrefixes = listOf("12210", "12240"))
        val request = capturedRequest(clientReturning(emptyList()), criteria(region = region))

        val filters =
            request
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .filter()
        assertThat(filters).hasSize(4)
        val regionBool = filters[3].bool()
        assertThat(regionBool.minimumShouldMatch()).isEqualTo("1")
        assertThat(
            regionBool.should().map { it.prefix().field() to it.prefix().value() },
        ).containsExactly("areaCode" to "12")
        assertThat(regionBool.mustNot().map { it.prefix().field() to it.prefix().value() })
            .containsExactly("areaCode" to "12210", "areaCode" to "12240")
    }

    @Test
    fun `관심 테마·시군구·읍면동 일치는 constant_score should 로 각 가중치만큼 가산한다`() {
        val request = capturedRequest(clientReturning(emptyList()), criteria())

        val should =
            request
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .should()
        assertThat(should).hasSize(3)
        assertConstantScore(should[0], "category", listOf("CAFETOUR", "FOOD"), 4.5f)
        assertConstantScore(should[1], "sigunguCode", listOf("11680"), 3.5f)
        assertConstantScore(should[2], "areaCode", listOf("1168010100"), 2.5f)
    }

    @Test
    fun `관심 항목이 비면 should 없이 조회한다`() {
        val request =
            capturedRequest(
                clientReturning(emptyList()),
                criteria(likeThemes = emptyList(), sigunguCodes = emptyList(), dongCodes = emptyList()),
            )

        assertThat(
            request
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .should(),
        ).isEmpty()
    }

    @Test
    fun `정렬은 _score desc, id desc 로 고정한다`() {
        val request = capturedRequest(clientReturning(emptyList()), criteria())

        val sort = request.sort()
        assertThat(sort).hasSize(2)
        assertThat(sort[0].isScore).isTrue()
        assertThat(sort[0].score().order()).isEqualTo(SortOrder.Desc)
        assertThat(sort[1].field().field()).isEqualTo("id")
        assertThat(sort[1].field().order()).isEqualTo(SortOrder.Desc)
    }

    @Test
    fun `after 가 있으면 점수와 id 로 search_after 를 넘기고 없으면 비운다`() {
        val first = capturedRequest(clientReturning(emptyList()), criteria())
        val next = capturedRequest(clientReturning(emptyList()), criteria(after = CourseRecommendAfter(8.25, 42L)))

        assertThat(first.searchAfterVals()).isEmpty()
        assertThat(next.searchAfterVals()).hasSize(2)
        assertThat(next.searchAfterVals()[0].doubleValue()).isEqualTo(8.25)
        assertThat(next.searchAfterVals()[1].longValue()).isEqualTo(42L)
    }

    @Test
    fun `장소 조건은 placeIds term 필터로, 제외 id 는 must_not ids 로 붙는다`() {
        val request =
            capturedRequest(clientReturning(emptyList()), criteria(placeId = 42L, excludeIds = listOf(1L, 2L)))

        val bool =
            request
                .query()!!
                .functionScore()
                .query()!!
                .bool()
        assertThat(bool.filter()).hasSize(4)
        assertThat(bool.filter()[3].term().field()).isEqualTo("placeIds")
        assertThat(
            bool
                .filter()[3]
                .term()
                .value()
                .stringValue(),
        ).isEqualTo("42")
        assertThat(bool.mustNot()).hasSize(1)
        assertThat(bool.mustNot()[0].ids().values()).containsExactly("1", "2")

        val plain =
            capturedRequest(clientReturning(emptyList()), criteria())
                .query()!!
                .functionScore()
                .query()!!
                .bool()
        assertThat(plain.mustNot()).isEmpty()
    }

    @Test
    fun `키워드는 제목 가중 multi_match 가산 should 로 붙고 비면 넣지 않는다`() {
        val should =
            capturedRequest(clientReturning(emptyList()), criteria(keyword = "조용한 산책"))
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .should()

        assertThat(should).hasSize(4)
        val keyword = should[1].multiMatch()
        assertThat(keyword.query()).isEqualTo("조용한 산책")
        assertThat(keyword.fields()).containsExactly("title^2", "description")
        assertThat(keyword.boost()).isEqualTo(3.0f)
        assertConstantScore(should[2], "sigunguCode", listOf("11680"), 3.5f)

        val blank =
            capturedRequest(clientReturning(emptyList()), criteria(keyword = "  "))
                .query()!!
                .functionScore()
                .query()!!
                .bool()
                .should()
        assertThat(blank).hasSize(3)
    }

    private fun assertConstantScore(
        query: Query,
        field: String,
        values: List<String>,
        boost: Float,
    ) {
        val cs = query.constantScore()
        assertThat(cs.boost()).isEqualTo(boost)
        val terms = cs.filter().terms()
        assertThat(terms.field()).isEqualTo(field)
        assertThat(terms.terms().value().map { it.stringValue() }).containsExactlyElementsOf(values)
    }
}
