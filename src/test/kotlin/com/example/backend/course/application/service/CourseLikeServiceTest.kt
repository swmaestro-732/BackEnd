package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.outbound.CourseLikePersistencePort
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * 코스 좋아요 서비스 단위 테스트 — 원자적 증감(UPDATE … RETURNING)과 실패 경로(코스 없음 404·중복 409·취소 멱등)를 검증한다.
 * CourseReviewService 처럼 인증된 userId 를 신뢰하므로 사용자 검증·잠금은 없다.
 */
class CourseLikeServiceTest {
    private val likePort = mock(CourseLikePersistencePort::class.java)
    private val coursePort = mock(CoursePersistencePort::class.java)
    private val service = CourseLikeService(likePort, coursePort)

    // --- like ---

    @Test
    fun `좋아요는 삽입 후 원자적 증가가 돌려준 likes_cnt 를 반환한다`() {
        `when`(likePort.existsLike(1L, 42L)).thenReturn(false)
        `when`(coursePort.increaseLikesCountReturning(42L)).thenReturn(4)

        val result = service.like(userId = 1L, courseId = 42L)

        verify(likePort).insert(1L, 42L)
        assertEquals(4, result)
    }

    @Test
    fun `이미 좋아요한 코스면 COURSE_ALREADY_LIKED 를 던지고 삽입하지 않는다`() {
        `when`(likePort.existsLike(1L, 42L)).thenReturn(true)

        val ex = assertThrows<BusinessException> { service.like(userId = 1L, courseId = 42L) }

        assertEquals(CourseErrorCode.COURSE_ALREADY_LIKED, ex.errorCode)
        verify(likePort, never()).insert(1L, 42L)
        verify(coursePort, never()).increaseLikesCountReturning(42L)
    }

    @Test
    fun `증가가 0행이면 COURSE_NOT_FOUND 를 던진다 (비활성 코스)`() {
        `when`(likePort.existsLike(1L, 42L)).thenReturn(false)
        `when`(coursePort.increaseLikesCountReturning(42L)).thenReturn(null)

        val ex = assertThrows<BusinessException> { service.like(userId = 1L, courseId = 42L) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
    }

    // --- unlike ---

    @Test
    fun `취소는 실제로 지웠을 때 원자적 감소가 돌려준 likes_cnt 를 반환한다`() {
        `when`(likePort.deleteByUserAndCourse(1L, 42L)).thenReturn(true)
        `when`(coursePort.decreaseLikesCountReturning(42L)).thenReturn(4)

        val result = service.unlike(userId = 1L, courseId = 42L)

        verify(coursePort, never()).readLikesCount(42L)
        assertEquals(4, result)
    }

    @Test
    fun `좋아요돼 있지 않은 코스 취소는 감소 없이 현재 likes_cnt 를 반환한다 (멱등)`() {
        `when`(likePort.deleteByUserAndCourse(1L, 42L)).thenReturn(false)
        `when`(coursePort.readLikesCount(42L)).thenReturn(5)

        val result = service.unlike(userId = 1L, courseId = 42L)

        verify(coursePort, never()).decreaseLikesCountReturning(42L)
        assertEquals(5, result)
    }

    @Test
    fun `취소 시 코스가 활성이 아니면 COURSE_NOT_FOUND 를 던진다`() {
        `when`(likePort.deleteByUserAndCourse(1L, 42L)).thenReturn(false)
        `when`(coursePort.readLikesCount(42L)).thenReturn(null)

        val ex = assertThrows<BusinessException> { service.unlike(userId = 1L, courseId = 42L) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, ex.errorCode)
    }

    // --- purgeByUser ---

    @Test
    fun `purgeByUser 는 실제 삭제된 코스들의 카운터를 배치로 내린다`() {
        `when`(likePort.deleteAllByUser(1L)).thenReturn(listOf(10L, 20L))

        service.purgeByUser(1L)

        verify(coursePort).decreaseLikesCounts(listOf(10L, 20L))
    }

    @Test
    fun `purgeByUser 는 좋아요가 없으면 카운터를 건드리지 않는다`() {
        `when`(likePort.deleteAllByUser(1L)).thenReturn(emptyList())

        service.purgeByUser(1L)

        verify(coursePort, never()).decreaseLikesCounts(org.mockito.ArgumentMatchers.anyList())
    }
}
