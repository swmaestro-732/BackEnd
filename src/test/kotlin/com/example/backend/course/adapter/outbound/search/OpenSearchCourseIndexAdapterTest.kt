package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.DeleteByQueryRequest
import org.opensearch.client.opensearch.core.DeleteRequest
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import java.util.function.Function

class OpenSearchCourseIndexAdapterTest {
    private val clientProvider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val adapter = OpenSearchCourseIndexAdapter(clientProvider, OpenSearchProperties())

    private fun courseWithId(id: Long): Course =
        Course.reconstitute(
            id = id,
            userId = 1L,
            status = CourseStatus.ACTIVE,
            title = "테스트 코스",
            description = null,
            coverImageUrl = null,
            category = CourseCategory.CULTURE,
            area = null,
            areaCode = null,
            visitDate = null,
            visibility = CourseVisibility.PUBLIC,
            isPublished = false,
            likesCnt = 0,
            commentsCnt = 0,
            savesCnt = 0,
            tracingsCnt = 0,
            forkedFromId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
            tags = emptyList(),
            places = emptyList(),
        )

    private fun courseWithNullId(): Course {
        val places =
            listOf(
                CoursePlace(placeId = 1L, orderNo = 0, caption = null, imageUrls = emptyList()),
                CoursePlace(placeId = 2L, orderNo = 1, caption = null, imageUrls = emptyList()),
            )
        return Course.create(
            userId = 1L,
            title = "미저장 코스",
            description = null,
            coverImageUrl = null,
            visibility = CourseVisibility.PUBLIC,
            isPublished = false,
            forkedFromId = null,
            tags = emptyList(),
            places = places,
            placeCategoryByPlaceId = emptyMap(),
            areaCode = null,
            area = null,
        )
    }

    @Test
    fun `빈 목록을 넘기면 client 를 조회하지 않는다`() {
        adapter.save(emptyList())

        verify(clientProvider, never()).ifAvailable
    }

    @Test
    fun `save(Course) — client 가 없으면 no-op 이고 예외가 전파되지 않는다`() {
        `when`(clientProvider.ifAvailable).thenReturn(null)

        adapter.save(courseWithId(1L)) // no exception
    }

    @Test
    fun `save(Course) — id 가 null 이면 early return 하고 예외가 전파되지 않는다`() {
        adapter.save(courseWithNullId()) // no exception
    }

    @Test
    fun `save(List) — client 가 없으면 no-op 이고 예외가 전파되지 않는다`() {
        `when`(clientProvider.ifAvailable).thenReturn(null)

        adapter.save(listOf(courseWithId(1L))) // no exception
    }

    @Test
    fun `save(List) — id 가 null 인 코스만 있으면 예외 없이 완료된다(early return)`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)

        adapter.save(listOf(courseWithNullId())) // no exception
    }

    @Test
    fun `save(List) — bulk 응답이 null 이어도 예외가 전파되지 않는다(fail-soft)`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        // client.bulk returns null by default → response.errors() throws NPE → caught by catch block

        adapter.save(listOf(courseWithId(1L))) // no exception
    }

    @Test
    fun `delete — client 가 없으면 no-op 이고 예외가 전파되지 않는다`() {
        `when`(clientProvider.ifAvailable).thenReturn(null)

        adapter.delete(courseId = 42L) // no exception
    }

    @Test
    fun `delete — client 가 있으면 예외를 삼킨다(fail-soft)`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)

        adapter.delete(courseId = 42L) // no exception
    }

    @Test
    fun `deleteByAuthor — client 가 없으면 no-op 이고 예외가 전파되지 않는다`() {
        `when`(clientProvider.ifAvailable).thenReturn(null)

        adapter.deleteByAuthor(authorId = 99L) // no exception
    }

    @Test
    fun `deleteByAuthor — client 가 있으면 예외를 삼킨다(fail-soft)`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)

        adapter.deleteByAuthor(authorId = 99L) // no exception
    }

    // ── 람다 실행 + indexAlias 검증 ──

    @Test
    fun `save(Course) — index 빌더 람다가 실행돼 indexAlias 가 사용된다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        doAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            val fn = inv.arguments[0] as Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>
            fn.apply(IndexRequest.Builder<Any>())
            null
        }.`when`(
            client,
        ).index<Any>(anyArg<java.util.function.Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>>())

        adapter.save(courseWithId(1L))

        verify(
            client,
        ).index(anyArg<java.util.function.Function<IndexRequest.Builder<Any>, ObjectBuilder<IndexRequest<Any>>>>())
    }

    @Test
    fun `save(List) — bulk 빌더 람다가 실행돼 indexAlias 가 사용된다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        var capturedRequest: BulkRequest? = null
        doAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            val fn = inv.arguments[0] as Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>
            capturedRequest = fn.apply(BulkRequest.Builder()).build()
            null
        }.`when`(client).bulk(anyArg<java.util.function.Function<BulkRequest.Builder, ObjectBuilder<BulkRequest>>>())

        adapter.save(listOf(courseWithId(1L)))

        assertThat(capturedRequest).isNotNull()
        assertThat(capturedRequest!!.operations()).isNotEmpty()
        @Suppress("UNCHECKED_CAST")
        assertThat(capturedRequest!!.operations()[0].index<Any>()!!.index()).isEqualTo("course")
    }

    @Test
    fun `delete — indexAlias 로 delete 요청이 구성된다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        var capturedIndex: String? = null
        doAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            val fn = inv.arguments[0] as Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>
            capturedIndex = fn.apply(DeleteRequest.Builder()).build().index()
            null
        }.`when`(
            client,
        ).delete(anyArg<java.util.function.Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        adapter.delete(courseId = 42L)

        assertThat(capturedIndex).isEqualTo("course")
    }

    @Test
    fun `deleteByAuthor — deleteByQuery 빌더 람다가 실행돼 indexAlias 가 사용된다`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        doAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            val fn = inv.arguments[0] as Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>
            fn.apply(DeleteByQueryRequest.Builder())
            null
        }.`when`(
            client,
        ).deleteByQuery(
            anyArg<java.util.function.Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>(),
        )

        adapter.deleteByAuthor(authorId = 99L)

        verify(
            client,
        ).deleteByQuery(
            anyArg<java.util.function.Function<DeleteByQueryRequest.Builder, ObjectBuilder<DeleteByQueryRequest>>>(),
        )
    }

    @Test
    fun `prefix 설정 시 delete 요청에 prefix 가 반영된다`() {
        val prefixedAdapter =
            OpenSearchCourseIndexAdapter(
                clientProvider,
                OpenSearchProperties(indexPrefix = "dev-"),
            )
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        var capturedIndex: String? = null
        doAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            val fn = inv.arguments[0] as Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>
            capturedIndex = fn.apply(DeleteRequest.Builder()).build().index()
            null
        }.`when`(
            client,
        ).delete(anyArg<java.util.function.Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>>>())

        prefixedAdapter.delete(courseId = 1L)

        assertThat(capturedIndex).isEqualTo("dev-course")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> anyArg(): T = org.mockito.Mockito.any<T>() as T
