package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.event.CourseActivityEvent
import com.example.backend.course.application.port.inbound.CourseActivityKind
import com.example.backend.course.application.port.inbound.dto.CreateCourseReviewCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseReviewPersistencePort
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * [CourseReviewService] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * 검증 대상: 코스 존재 검증(없으면 404), 도메인 조립 후 영속 포트 위임, 삭제 0건의 404 은닉.
 * 도메인 [CourseReview] 는 data class 라 [CourseReview.create] 로 만든 기대값과 동등성 비교로 스텁한다(매처 불필요).
 * (태그 코드 → enum 변환은 웹 어댑터 toCommand 책임 — 컨트롤러 테스트가 커버한다.)
 */
class CourseReviewServiceTest {
    private val coursePersistencePort = mock(CoursePersistencePort::class.java)
    private val reviewPersistencePort = mock(CourseReviewPersistencePort::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val occurredAt = Instant.parse("2026-10-24T10:09:59Z")
    private val clock = Clock.fixed(occurredAt, ZoneOffset.UTC)
    private val service = CourseReviewService(coursePersistencePort, reviewPersistencePort, eventPublisher, clock)

    @Test
    fun `리뷰를 저장하고 생성된 id 를 담은 도메인을 돌려준다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(true)
        val expected =
            review(
                rating = 5,
                content = "  동선이 편했어요  ", // 트림은 도메인 팩토리가 맡는다 — 기대값도 같은 팩토리로 만든다
                photoUrls = listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
                tags = setOf(CourseReviewTag.PACKED, CourseReviewTag.SMOOTH),
            )
        `when`(reviewPersistencePort.save(expected)).thenReturn(saved(expected))

        val created =
            service.create(
                command(
                    rating = 5,
                    content = "  동선이 편했어요  ",
                    photoUrls = listOf("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg"),
                    tags = setOf(CourseReviewTag.PACKED, CourseReviewTag.SMOOTH),
                ),
            )

        assertEquals(SAVED_REVIEW_ID, created.id)
        assertEquals("동선이 편했어요", created.content)
        val order = inOrder(reviewPersistencePort, eventPublisher)
        order.verify(reviewPersistencePort).save(expected)
        order
            .verify(
                eventPublisher,
            ).publishEvent(CourseActivityEvent(CourseActivityKind.REVIEWED, USER_ID, COURSE_ID, occurredAt))
        verifyNoMoreInteractions(eventPublisher)
    }

    @Test
    fun `별점만 남겨도 저장된다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(true)
        val expected = review(rating = 3)
        `when`(reviewPersistencePort.save(expected)).thenReturn(saved(expected))

        service.create(command(rating = 3))

        verify(reviewPersistencePort).save(expected) // content·사진·태그가 빈 도메인 그대로다
    }

    @Test
    fun `없는 코스에는 리뷰를 쓸 수 없다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(false)

        val exception = assertThrows<BusinessException> { service.create(command()) }

        assertEquals(CourseErrorCode.COURSE_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(reviewPersistencePort, eventPublisher)
    }

    @Test
    fun `리뷰 이벤트의 사용자와 코스는 저장 결과를 따른다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(true)
        val expected = review()
        `when`(reviewPersistencePort.save(expected)).thenReturn(saved(expected, courseId = 702L, userId = 2L))

        service.create(command())

        verify(eventPublisher).publishEvent(CourseActivityEvent(CourseActivityKind.REVIEWED, 2L, 702L, occurredAt))
        verifyNoMoreInteractions(eventPublisher)
    }

    @Test
    fun `이미 작성한 리뷰가 있으면 저장하거나 이벤트를 발행하지 않는다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(true)
        `when`(reviewPersistencePort.existsActiveReview(COURSE_ID, USER_ID)).thenReturn(true)

        val exception = assertThrows<BusinessException> { service.create(command()) }

        assertEquals(CourseErrorCode.COURSE_REVIEW_ALREADY_EXISTS, exception.errorCode)
        verify(reviewPersistencePort, never()).save(review())
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `리뷰 저장에 실패하면 이벤트를 발행하지 않는다`() {
        `when`(coursePersistencePort.existsById(COURSE_ID)).thenReturn(true)
        `when`(reviewPersistencePort.save(review())).thenThrow(IllegalStateException("저장 실패"))

        assertThrows<IllegalStateException> { service.create(command()) }

        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `삭제는 소프트 삭제 결과가 1건이면 조용히 끝난다`() {
        `when`(reviewPersistencePort.softDelete(reviewId = REVIEW_ID, courseId = COURSE_ID, userId = USER_ID))
            .thenReturn(1)

        service.delete(userId = USER_ID, courseId = COURSE_ID, reviewId = REVIEW_ID)

        verify(reviewPersistencePort).softDelete(reviewId = REVIEW_ID, courseId = COURSE_ID, userId = USER_ID)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `없는·타인·이미 삭제된 리뷰는 사유 구분 없이 4046 으로 은닉한다`() {
        `when`(reviewPersistencePort.softDelete(reviewId = REVIEW_ID, courseId = COURSE_ID, userId = USER_ID))
            .thenReturn(0)

        val exception =
            assertThrows<BusinessException> {
                service.delete(userId = USER_ID, courseId = COURSE_ID, reviewId = REVIEW_ID)
            }

        assertEquals(CourseErrorCode.COURSE_REVIEW_NOT_FOUND, exception.errorCode)
    }

    private fun command(
        courseId: Long = COURSE_ID,
        rating: Int = 4,
        content: String? = null,
        photoUrls: List<String> = emptyList(),
        tags: Set<CourseReviewTag> = emptySet(),
    ) = CreateCourseReviewCommand(
        courseId = courseId,
        userId = USER_ID,
        rating = rating,
        content = content,
        photoUrls = photoUrls,
        tags = tags,
    )

    private fun review(
        rating: Int = 4,
        content: String? = null,
        photoUrls: List<String> = emptyList(),
        tags: Set<CourseReviewTag> = emptySet(),
    ) = CourseReview.create(
        courseId = COURSE_ID,
        userId = USER_ID,
        rating = rating,
        content = content,
        photoUrls = photoUrls,
        tags = tags,
    )

    private fun saved(
        review: CourseReview,
        courseId: Long = review.courseId,
        userId: Long = review.userId,
    ) = CourseReview.reconstitute(
        id = SAVED_REVIEW_ID,
        courseId = courseId,
        userId = userId,
        status = review.status,
        rating = review.rating,
        content = review.content,
        photoUrls = review.photoUrls,
        tags = review.tags,
        createdAt = kotlin.time.Instant.parse("2026-10-24T10:09:59Z"),
    )

    private companion object {
        const val COURSE_ID = 701L
        const val USER_ID = 1L
        const val REVIEW_ID = 11L
        const val SAVED_REVIEW_ID = 100L
    }
}
