package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CoursePlaceImageRow
import com.example.backend.course.application.port.outbound.CoursePlaceRow
import com.example.backend.course.application.port.outbound.CourseSummaryRow
import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import com.example.backend.course.application.port.outbound.ViewerCourseState
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import com.example.backend.course.domain.model.CourseCategory
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class CourseQueryServiceTest {
    private val persistence = mock(CoursePersistencePort::class.java)
    private val tags = mock(CourseTagQueryPort::class.java)
    private val interactions = mock(ViewerInteractionPort::class.java)
    private val service = CourseQueryService(persistence, tags, interactions)

    @Test
    fun `단건 상세는 장소 조회자 상태 태그를 조립한다`() {
        val row = detail(10L, CourseVisibility.PUBLIC)
        val place = CoursePlaceRow(1L, 30L, 0, "첫 장소", 5, listOf(CoursePlaceImageRow("image", 0)))
        `when`(persistence.findCourseDetails(listOf(10L))).thenReturn(listOf(row))
        `when`(persistence.findPlacesByCourseIds(listOf(10L))).thenReturn(mapOf(10L to listOf(place)))
        `when`(interactions.getViewerStates(7L, listOf(10L))).thenReturn(listOf(ViewerCourseState(10L, true, true)))
        `when`(tags.findTagNamesByCourseId(10L)).thenReturn(listOf("데이트"))

        val result = service.getDetail(10L, 7L)

        assertEquals(listOf("데이트"), result.tags)
        assertEquals(30L, result.places.single().placeId)
        assertEquals(
            "image",
            result.places
                .single()
                .images
                .single()
                .imageUrl,
        )
        assertTrue(result.hasSaved)
        assertTrue(result.hasStartedCourse)
    }

    @Test
    fun `비로그인 조회에서는 비공개 비활성 코스를 제외하고 입력 순서를 유지한다`() {
        `when`(persistence.findCourseDetails(listOf(3L, 1L, 2L)))
            .thenReturn(
                listOf(
                    detail(1L, CourseVisibility.PUBLIC),
                    detail(2L, CourseVisibility.PRIVATE),
                    detail(3L, CourseVisibility.PUBLIC, CourseStatus.DELETED),
                ),
            )
        `when`(persistence.findPlacesByCourseIds(listOf(1L))).thenReturn(emptyMap())

        val result = service.getDetails(listOf(3L, 1L, 2L), null)

        assertEquals(listOf(1L), result.map { it.id })
        verifyNoInteractions(interactions)
    }

    @Test
    fun `조회할 수 없는 단건은 코스 없음 예외를 던진다`() {
        `when`(persistence.findCourseDetails(listOf(10L))).thenReturn(listOf(detail(10L, CourseVisibility.PRIVATE)))

        val exception = assertThrows(BusinessException::class.java) { service.getDetail(10L, 7L) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(tags)
    }

    @Test
    fun `팔로워의 작성자 목록은 공개와 팔로워 공개범위를 조회한다`() {
        `when`(interactions.isFollowing(7L, 3L)).thenReturn(true)
        `when`(
            persistence.findPublishedByAuthor(
                3L,
                setOf(CourseVisibility.PUBLIC, CourseVisibility.FOLLOWER),
                null,
                2,
            ),
        ).thenReturn(listOf(summary(1L), summary(2L), summary(3L)))

        val result = service.listByAuthor(3L, 7L, null, 2)

        assertEquals(listOf(1L, 2L), result.items.map { it.id })
        assertTrue(result.hasNext)
    }

    @Test
    fun `공개 목록 크기는 최소 1로 보정한다`() {
        `when`(persistence.findPublishedPublic(null, 1)).thenReturn(listOf(summary(1L)))

        val result = service.listPublic(null, 0)

        assertEquals(listOf(1L), result.items.map { it.id })
        assertFalse(result.hasNext)
        verify(persistence).findPublishedPublic(null, 1)
    }

    private fun detail(
        id: Long,
        visibility: CourseVisibility,
        status: CourseStatus = CourseStatus.ACTIVE,
    ) = com.example.backend.course.application.port.outbound.CourseDetailRow(
        id = id,
        userId = 3L,
        title = "코스 $id",
        coverImageUrl = null,
        description = null,
        category = CourseCategory.CAFETOUR,
        area = "성수",
        areaCode = "1120011400",
        tracingsCnt = 1,
        status = status,
        visibility = visibility,
        isPublished = true,
    )

    private fun summary(id: Long) =
        CourseSummaryRow(
            id,
            3L,
            "코스 $id",
            null,
            CourseCategory.CAFETOUR,
            CourseVisibility.PUBLIC,
            true,
            1,
            2,
            Instant.parse("2026-09-01T00:00:00Z"),
        )
}
