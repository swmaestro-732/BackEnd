package com.example.backend.course.adapter.outbound.search

import com.example.backend.common.exception.BusinessException
import com.example.backend.course.application.port.inbound.dto.CourseSearchSort
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.opensearch.core.search.HitsMetadata
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant

/**
 * [OpenSearchCourseSearchAdapter] 단위 테스트 — [OpenSearchClient] 를 목킹해 어댑터의 응답 매핑·hasNext 판정·
 * nextCursor 생성·정렬 축별 커서·필터·fail-soft 를 검증한다(무거운 Testcontainers 통합테스트와 별개, jacoco `test` 포함).
 */
class OpenSearchCourseSearchAdapterTest {
    private fun doc(
        id: Long,
        title: String = "코스 $id",
        category: String? = "CAFETOUR",
        area: String? = "성수",
        userId: String = "7",
        likesCnt: Int = 3,
        savesCnt: Int = 11,
        createdAt: Long? = 1000L,
    ) = CourseDocument(
        id = id,
        title = title,
        description = "설명",
        area = area,
        category = category,
        tags = listOf("데이트"),
        coverImageUrl = "https://img/$id.jpg",
        visibility = "PUBLIC",
        isPublished = true,
        userId = userId,
        likesCnt = likesCnt,
        savesCnt = savesCnt,
        createdAt = createdAt,
    )

    @Suppress("UNCHECKED_CAST")
    private fun hit(
        doc: CourseDocument,
        score: Double? = 1.5,
    ): Hit<CourseDocument> {
        val h = mock(Hit::class.java) as Hit<CourseDocument>
        `when`(h.source()).thenReturn(doc)
        `when`(h.score()).thenReturn(score)
        return h
    }

    @Suppress("UNCHECKED_CAST")
    private fun adapterReturning(hits: List<Hit<CourseDocument>>): OpenSearchCourseSearchAdapter {
        val client = mock(OpenSearchClient::class.java)
        val response = mock(SearchResponse::class.java) as SearchResponse<CourseDocument>
        val meta = mock(HitsMetadata::class.java) as HitsMetadata<CourseDocument>
        `when`(response.hits()).thenReturn(meta)
        `when`(meta.hits()).thenReturn(hits)
        `when`(client.search(any(SearchRequest::class.java), eq(CourseDocument::class.java))).thenReturn(response)
        return OpenSearchCourseSearchAdapter(providerOf(client))
    }

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun criteria(
        sort: CourseSearchSort = CourseSearchSort.RELEVANCE,
        size: Int = 2,
        cursor: String? = null,
        keyword: String? = "카페",
        area: String? = "성수",
        category: String? = "CAFETOUR",
        tags: List<String> = listOf("데이트"),
    ) = CourseSearchCriteria(
        keyword = keyword,
        area = area,
        category = category,
        tags = tags,
        sort = sort,
        cursor = cursor,
        size = size,
    )

    @Test
    fun `클라이언트가 없으면 빈 페이지를 돌려준다`() {
        val adapter = OpenSearchCourseSearchAdapter(providerOf(null))

        val page = adapter.search(criteria())

        assertThat(page.hits).isEmpty()
        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `size 초과 히트가 있으면 hasNext true 로 잘라내고 toRow 로 매핑한다`() {
        val adapter = adapterReturning(listOf(hit(doc(1)), hit(doc(2)), hit(doc(3))))

        val page = adapter.search(criteria(sort = CourseSearchSort.RELEVANCE, size = 2))

        assertThat(page.hits.map { it.id }).containsExactly(1L, 2L)
        assertThat(page.hasNext).isTrue()
        assertThat(page.nextCursor).isNotNull()
        val row = page.hits.first()
        assertThat(row.authorId).isEqualTo(7L)
        assertThat(row.title).isEqualTo("코스 1")
        assertThat(row.theme).isEqualTo("CAFETOUR")
        assertThat(row.area).isEqualTo("성수")
        assertThat(row.coverImageUrl).isEqualTo("https://img/1.jpg")
        assertThat(row.likesCnt).isEqualTo(3)
        assertThat(row.savesCnt).isEqualTo(11)
        assertThat(row.createdAt).isEqualTo(Instant.ofEpochMilli(1000L))
    }

    @Test
    fun `RELEVANCE 커서는 마지막 히트 score 로 인코딩된다`() {
        val last = hit(doc(2), score = 4.25)
        val adapter = adapterReturning(listOf(hit(doc(1)), last, hit(doc(3))))

        val page = adapter.search(criteria(sort = CourseSearchSort.RELEVANCE, size = 2))

        val after = CourseSearchCursorCodec.decode(CourseSearchSort.RELEVANCE, page.nextCursor)
        assertThat(after).containsExactly(4.25, 2L)
    }

    @Test
    fun `LATEST 커서는 마지막 히트 createdAt 로 인코딩된다`() {
        val last = hit(doc(2, createdAt = 2000L))
        val adapter = adapterReturning(listOf(hit(doc(1)), last, hit(doc(3))))

        val page = adapter.search(criteria(sort = CourseSearchSort.LATEST, size = 2))

        val after = CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, page.nextCursor)
        assertThat(after).containsExactly(2000L, 2L)
    }

    @Test
    fun `POPULAR 커서는 마지막 히트 savesCnt 로 인코딩된다`() {
        val last = hit(doc(2, savesCnt = 42))
        val adapter = adapterReturning(listOf(hit(doc(1)), last, hit(doc(3))))

        val page = adapter.search(criteria(sort = CourseSearchSort.POPULAR, size = 2))

        val after = CourseSearchCursorCodec.decode(CourseSearchSort.POPULAR, page.nextCursor)
        assertThat(after).containsExactly(42L, 2L)
    }

    @Test
    fun `다음 페이지가 없으면 hasNext false 와 nextCursor null`() {
        val adapter = adapterReturning(listOf(hit(doc(1))))

        val page = adapter.search(criteria(size = 2))

        assertThat(page.hits.map { it.id }).containsExactly(1L)
        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `유효한 커서는 search_after 로 이어 조회한다`() {
        val adapter = adapterReturning(listOf(hit(doc(1))))
        val cursor = CourseSearchCursorCodec.encode(CourseSearchSort.LATEST, 5000L, 9L)

        val page = adapter.search(criteria(sort = CourseSearchSort.LATEST, size = 2, cursor = cursor))

        assertThat(page.hits.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `키워드와 필터가 비어도 match_all 로 조회한다`() {
        val adapter = adapterReturning(listOf(hit(doc(1))))

        val page =
            adapter.search(
                criteria(keyword = null, area = null, category = null, tags = emptyList()),
            )

        assertThat(page.hits.map { it.id }).containsExactly(1L)
    }

    @Test
    fun `잘못된 커서는 fail-soft 로 삼키지 않고 예외로 드러난다`() {
        val adapter = OpenSearchCourseSearchAdapter(providerOf(mock(OpenSearchClient::class.java)))

        assertThatThrownBy {
            adapter.search(criteria(sort = CourseSearchSort.LATEST, cursor = "not-a-valid-cursor!!"))
        }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `검색 중 예외가 나면 빈 페이지로 흡수한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.search(any(SearchRequest::class.java), eq(CourseDocument::class.java)))
            .thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseSearchAdapter(providerOf(client))

        val page = adapter.search(criteria())

        assertThat(page.hits).isEmpty()
        assertThat(page.hasNext).isFalse()
        assertThat(page.nextCursor).isNull()
    }
}
