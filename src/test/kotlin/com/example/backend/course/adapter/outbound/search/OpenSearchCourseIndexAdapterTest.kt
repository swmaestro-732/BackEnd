package com.example.backend.course.adapter.outbound.search

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.ErrorCause
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.DeleteByQueryRequest
import org.opensearch.client.opensearch.core.DeleteByQueryResponse
import org.opensearch.client.opensearch.core.DeleteRequest
import org.opensearch.client.opensearch.core.DeleteResponse
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.IndexResponse
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.util.function.Function

private typealias IndexFn = Function<IndexRequest.Builder<CourseDocument>, ObjectBuilder<IndexRequest<CourseDocument>>>

/**
 * [OpenSearchCourseIndexAdapter] 단위 테스트 — [OpenSearchClient] 를 목킹해 save/bulk/delete/deleteByAuthor 의
 * 요청 구성(toDocument 매핑 포함)·no-op(클라이언트 없음)·부분 실패 집계·fail-soft 예외 흡수를 검증한다.
 * 목 client 는 넘겨진 빌더 람다를 실제로 적용(thenAnswer)해 요청 구성 코드가 실행되게 한다.
 */
class OpenSearchCourseIndexAdapterTest {
    // any() 의 T 를 각 메서드의 정확한 Function 시그니처로 고정하는 매처 헬퍼(복합 제네릭이라 추론 불가).
    // 반환 타입은 nullable — any() 는 null 을 돌려주므로 non-null 이면 코틀린 인트린식 null 체크가 NPE 를 낸다.
    private fun anyIndexFn(): IndexFn? = any()

    private fun anyBulkFn(): Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>? = any()

    private fun anyDeleteFn(): Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>? = any()

    private fun anyDbqFn(): Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>? = any()

    private fun providerOf(client: OpenSearchClient?): ObjectProvider<OpenSearchClient> {
        @Suppress("UNCHECKED_CAST")
        val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
        `when`(provider.ifAvailable).thenReturn(client)
        return provider
    }

    private fun course(
        id: Long,
        visibility: CourseVisibility = CourseVisibility.PUBLIC,
    ): Course =
        Course.reconstitute(
            id = id,
            userId = 7L,
            status = CourseStatus.ACTIVE,
            title = "코스 $id",
            description = "설명",
            coverImageUrl = "https://img.jpg",
            category = null,
            area = "성수",
            areaCode = null,
            visitDate = null,
            visibility = visibility,
            isPublished = true,
            likesCnt = 1,
            commentsCnt = 0,
            savesCnt = 2,
            tracingsCnt = 0,
            forkedFromId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
            tags = listOf("데이트"),
            places = emptyList(),
        )

    @Test
    fun `save 는 클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null))

        assertThatCode { adapter.save(course(id = 1L)) }.doesNotThrowAnyException()
    }

    @Test
    fun `save 는 클라이언트가 있으면 문서를 색인한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.index(anyIndexFn())).thenAnswer { inv ->
            inv.getArgument<Function<IndexRequest.Builder<CourseDocument>, *>>(0).apply(IndexRequest.Builder())
            mock(IndexResponse::class.java)
        }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        adapter.save(course(id = 1L))

        verify(client).index(anyIndexFn())
    }

    @Test
    fun `save 는 색인 예외를 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.index(anyIndexFn())).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        assertThatCode { adapter.save(course(id = 1L)) }.doesNotThrowAnyException()
    }

    @Test
    fun `bulk save 는 빈 리스트면 no-op 한다`() {
        val client = mock(OpenSearchClient::class.java)
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        adapter.save(emptyList())

        verify(client, never()).bulk(anyBulkFn())
    }

    @Test
    fun `bulk save 는 클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null))

        assertThatCode { adapter.save(listOf(course(id = 1L))) }.doesNotThrowAnyException()
    }

    @Test
    fun `bulk save 는 문서들을 색인하고 성공하면 로그만 남긴다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.bulk(anyBulkFn())).thenAnswer { inv ->
            inv.getArgument<Function<BulkRequest.Builder, *>>(0).apply(BulkRequest.Builder())
            val resp = mock(BulkResponse::class.java)
            `when`(resp.errors()).thenReturn(false)
            resp
        }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        adapter.save(listOf(course(id = 1L), course(id = 2L)))

        verify(client).bulk(anyBulkFn())
    }

    @Test
    fun `bulk save 는 부분 실패를 집계해 경고한다`() {
        val client = mock(OpenSearchClient::class.java)
        val failedItem = mock(BulkResponseItem::class.java)
        `when`(failedItem.error()).thenReturn(mock(ErrorCause::class.java))
        `when`(client.bulk(anyBulkFn())).thenAnswer { inv ->
            inv.getArgument<Function<BulkRequest.Builder, *>>(0).apply(BulkRequest.Builder())
            val resp = mock(BulkResponse::class.java)
            `when`(resp.errors()).thenReturn(true)
            `when`(resp.items()).thenReturn(listOf(failedItem))
            resp
        }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        assertThatCode { adapter.save(listOf(course(id = 1L))) }.doesNotThrowAnyException()
    }

    @Test
    fun `bulk save 는 예외를 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.bulk(anyBulkFn())).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        assertThatCode { adapter.save(listOf(course(id = 1L))) }.doesNotThrowAnyException()
    }

    @Test
    fun `delete 는 클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null))

        assertThatCode { adapter.delete(1L) }.doesNotThrowAnyException()
    }

    @Test
    fun `delete 는 클라이언트가 있으면 문서를 삭제한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.delete(anyDeleteFn())).thenAnswer { inv ->
            inv.getArgument<Function<DeleteRequest.Builder, *>>(0).apply(DeleteRequest.Builder())
            mock(DeleteResponse::class.java)
        }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        adapter.delete(1L)

        verify(client).delete(anyDeleteFn())
    }

    @Test
    fun `delete 는 예외를 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.delete(anyDeleteFn())).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        assertThatCode { adapter.delete(1L) }.doesNotThrowAnyException()
    }

    @Test
    fun `deleteByAuthor 는 클라이언트가 없으면 no-op 한다`() {
        val adapter = OpenSearchCourseIndexAdapter(providerOf(null))

        assertThatCode { adapter.deleteByAuthor(7L) }.doesNotThrowAnyException()
    }

    @Test
    fun `deleteByAuthor 는 클라이언트가 있으면 작성자 문서를 삭제한다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.deleteByQuery(anyDbqFn())).thenAnswer { inv ->
            inv.getArgument<Function<DeleteByQueryRequest.Builder, *>>(0).apply(DeleteByQueryRequest.Builder())
            mock(DeleteByQueryResponse::class.java)
        }
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        adapter.deleteByAuthor(7L)

        verify(client).deleteByQuery(anyDbqFn())
    }

    @Test
    fun `deleteByAuthor 는 예외를 fail-soft 로 삼킨다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(client.deleteByQuery(anyDbqFn())).thenThrow(RuntimeException("boom"))
        val adapter = OpenSearchCourseIndexAdapter(providerOf(client))

        assertThatCode { adapter.deleteByAuthor(7L) }.doesNotThrowAnyException()
    }
}
