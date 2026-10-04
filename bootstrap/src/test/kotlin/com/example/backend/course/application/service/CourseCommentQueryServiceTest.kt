package com.example.backend.course.application.service

import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import com.example.backend.course.application.port.outbound.CourseCommentRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class CourseCommentQueryServiceTest {
    private val comments = mock(CourseCommentPersistencePort::class.java)
    private val service = CourseCommentQueryService(comments)

    @Test
    fun `추가 행이 있으면 요청 개수만 반환하고 다음 페이지를 표시한다`() {
        `when`(comments.findPage(10L, null, 2)).thenReturn(listOf(row(3L, 7L), row(2L, 8L), row(1L, 7L)))

        val result = service.list(10L, 7L, null, 2)

        assertEquals(listOf(3L, 2L), result.items.map { it.id })
        assertTrue(result.hasNext)
        assertTrue(result.items[0].isMine)
        assertFalse(result.items[1].isMine)
        assertEquals(7L, result.items[0].authorId)
        assertEquals("댓글 3", result.items[0].content)
        assertEquals(CREATED_AT, result.items[0].createdAt)
        assertEquals(UPDATED_AT, result.items[0].updatedAt)
    }

    @Test
    fun `커서를 전달하고 정확히 요청 개수만 있으면 마지막 페이지다`() {
        `when`(comments.findPage(10L, 3L, 2)).thenReturn(listOf(row(2L, 7L), row(1L, 8L)))

        val result = service.list(10L, null, 3L, 2)

        assertEquals(listOf(2L, 1L), result.items.map { it.id })
        assertFalse(result.hasNext)
        assertTrue(result.items.none { it.isMine })
        verify(comments).findPage(10L, 3L, 2)
    }

    @Test
    fun `댓글이 없으면 빈 마지막 페이지를 반환한다`() {
        `when`(comments.findPage(10L, null, 20)).thenReturn(emptyList())

        val result = service.list(10L, 7L, null, 20)

        assertTrue(result.items.isEmpty())
        assertFalse(result.hasNext)
    }

    @Test
    fun `페이지 크기는 최소 1로 보정한다`() {
        `when`(comments.findPage(10L, null, 1)).thenReturn(listOf(row(2L, 7L), row(1L, 7L)))

        val result = service.list(10L, 7L, null, 0)

        assertEquals(listOf(2L), result.items.map { it.id })
        assertTrue(result.hasNext)
        verify(comments).findPage(10L, null, 1)
    }

    @Test
    fun `페이지 크기는 최대 100으로 보정한다`() {
        `when`(comments.findPage(10L, null, 100)).thenReturn(emptyList())

        service.list(10L, null, null, Int.MAX_VALUE)

        verify(comments).findPage(10L, null, 100)
    }

    private fun row(
        id: Long,
        authorId: Long,
    ) = CourseCommentRow(id, authorId, "댓글 $id", CREATED_AT, UPDATED_AT)

    private companion object {
        val CREATED_AT: Instant = Instant.parse("2026-09-01T00:00:00Z")
        val UPDATED_AT: Instant = Instant.parse("2026-09-02T00:00:00Z")
    }
}
