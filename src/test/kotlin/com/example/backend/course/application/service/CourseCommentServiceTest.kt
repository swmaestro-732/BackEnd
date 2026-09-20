package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.dto.CreateCourseCommentCommand
import com.example.backend.course.application.port.inbound.dto.EditCourseCommentCommand
import com.example.backend.course.application.port.outbound.CourseCommentPersistencePort
import com.example.backend.course.application.port.outbound.CourseCommentRow
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class CourseCommentServiceTest {
    private val courses = mock(CoursePersistencePort::class.java)
    private val comments = mock(CourseCommentPersistencePort::class.java)
    private val service = CourseCommentService(courses, comments)

    @Test
    fun `댓글을 저장한 결과를 반환하고 코스 댓글 수를 증가시킨다`() {
        val row = CourseCommentRow(30L, 7L, "저장된 댓글", CREATED_AT, UPDATED_AT)
        `when`(courses.existsById(10L)).thenReturn(true)
        `when`(comments.save(10L, 7L, "요청 댓글")).thenReturn(row)
        `when`(courses.increaseCommentsCount(10L)).thenReturn(1)

        val result = service.create(CreateCourseCommentCommand(10L, 7L, "요청 댓글"))

        assertEquals(30L, result.id)
        assertEquals(7L, result.authorId)
        assertEquals("저장된 댓글", result.content)
        assertEquals(CREATED_AT, result.createdAt)
        assertEquals(UPDATED_AT, result.updatedAt)
        assertTrue(result.isMine)
        val order = inOrder(courses, comments)
        order.verify(courses).existsById(10L)
        order.verify(comments).save(10L, 7L, "요청 댓글")
        order.verify(courses).increaseCommentsCount(10L)
    }

    @Test
    fun `없는 코스에는 댓글을 저장하거나 카운터를 증가시키지 않는다`() {
        `when`(courses.existsById(10L)).thenReturn(false)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(CreateCourseCommentCommand(10L, 7L, "댓글"))
            }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(comments)
        verify(courses, never()).increaseCommentsCount(10L)
    }

    @Test
    fun `댓글 저장 중 코스가 삭제되면 예외로 트랜잭션을 실패시킨다`() {
        `when`(courses.existsById(10L)).thenReturn(true)
        `when`(comments.save(10L, 7L, "댓글"))
            .thenReturn(CourseCommentRow(30L, 7L, "댓글", CREATED_AT, CREATED_AT))
        `when`(courses.increaseCommentsCount(10L)).thenReturn(0)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.create(CreateCourseCommentCommand(10L, 7L, "댓글"))
            }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `댓글 편집은 코스와 작성자를 함께 전달하고 카운터를 바꾸지 않는다`() {
        `when`(comments.updateContent(10L, 30L, 7L, "수정 댓글")).thenReturn(1)

        service.edit(EditCourseCommentCommand(10L, 30L, 7L, "수정 댓글"))

        verify(comments).updateContent(10L, 30L, 7L, "수정 댓글")
        verifyNoInteractions(courses)
    }

    @Test
    fun `없는 댓글이나 타인 댓글 편집은 댓글 없음으로 숨긴다`() {
        `when`(comments.updateContent(10L, 30L, 7L, "수정 댓글")).thenReturn(0)

        val exception =
            assertThrows(BusinessException::class.java) {
                service.edit(EditCourseCommentCommand(10L, 30L, 7L, "수정 댓글"))
            }

        assertEquals(CourseErrorCode.COURSE_COMMENT_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(courses)
    }

    @Test
    fun `댓글 삭제에 성공한 뒤에만 카운터를 감소시킨다`() {
        `when`(comments.softDelete(10L, 30L, 7L)).thenReturn(1)
        `when`(courses.decreaseCommentsCount(10L)).thenReturn(1)

        service.delete(7L, 10L, 30L)

        val order = inOrder(comments, courses)
        order.verify(comments).softDelete(10L, 30L, 7L)
        order.verify(courses).decreaseCommentsCount(10L)
    }

    @Test
    fun `없는 댓글이나 타인 댓글 삭제는 카운터를 감소시키지 않는다`() {
        `when`(comments.softDelete(10L, 30L, 7L)).thenReturn(0)

        val exception = assertThrows(BusinessException::class.java) { service.delete(7L, 10L, 30L) }

        assertEquals(CourseErrorCode.COURSE_COMMENT_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(courses)
    }

    @Test
    fun `이미 삭제된 코스의 댓글도 본인 댓글이면 삭제할 수 있다`() {
        `when`(comments.softDelete(10L, 30L, 7L)).thenReturn(1)
        `when`(courses.decreaseCommentsCount(10L)).thenReturn(0)

        service.delete(7L, 10L, 30L)

        verify(courses).decreaseCommentsCount(10L)
    }

    private companion object {
        val CREATED_AT: Instant = Instant.parse("2026-09-01T00:00:00Z")
        val UPDATED_AT: Instant = Instant.parse("2026-09-02T00:00:00Z")
    }
}
