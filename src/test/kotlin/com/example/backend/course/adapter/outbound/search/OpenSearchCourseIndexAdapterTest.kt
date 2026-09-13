package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CoursePlace
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider

class OpenSearchCourseIndexAdapterTest {
    private val clientProvider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val adapter = OpenSearchCourseIndexAdapter(clientProvider)

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
}
