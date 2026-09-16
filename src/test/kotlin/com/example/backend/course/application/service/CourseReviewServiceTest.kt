package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.dto.CreateCourseReviewCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseReviewPersistencePort
import com.example.backend.course.domain.model.CourseReview
import com.example.backend.course.domain.model.CourseReviewTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.time.Clock

/**
 * [CourseReviewService] 단위 테스트 — 포트를 목으로 대체해 서비스 규칙만 검증한다.
 * 검증 대상: 코스 존재 검증(없으면 404), 도메인 조립 후 영속 포트 위임, 삭제 0건의 404 은닉.
 * 도메인 [CourseReview] 는 data class 라 [CourseReview.create] 로 만든 기대값과 동등성 비교로 스텁한다(매처 불필요).
 * (태그 코드 → enum 변환은 웹 어댑터 toCommand 책임 — 컨트롤러 테스트가 커버한다.)
 */
class CourseReviewServiceTest {
    private val coursePersistencePort = mock(CoursePersistencePort::class.java)
    private val reviewPersistencePort = mock(CourseReviewPersistencePort::class.java)
    private val service = CourseReviewService(coursePersistencePort, reviewPersistencePort)

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
        verify(reviewPersistencePort).save(expected) // 커맨드가 도메인으로 그대로 조립돼 위임됐다
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
        verifyNoInteractions(reviewPersistencePort) // 저장까지 가지 않는다
    }

    @Test
    fun `삭제는 소프트 삭제 결과가 1건이면 조용히 끝난다`() {
        `when`(reviewPersistencePort.softDelete(reviewId = REVIEW_ID, courseId = COURSE_ID, userId = USER_ID))
            .thenReturn(1)

        service.delete(userId = USER_ID, courseId = COURSE_ID, reviewId = REVIEW_ID)

        verify(reviewPersistencePort).softDelete(reviewId = REVIEW_ID, courseId = COURSE_ID, userId = USER_ID)
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

    private fun saved(review: CourseReview) =
        CourseReview.reconstitute(
            id = SAVED_REVIEW_ID,
            courseId = review.courseId,
            userId = review.userId,
            status = review.status,
            rating = review.rating,
            content = review.content,
            photoUrls = review.photoUrls,
            tags = review.tags,
            createdAt = Clock.System.now(),
        )

    private companion object {
        const val COURSE_ID = 701L
        const val USER_ID = 1L
        const val REVIEW_ID = 11L
        const val SAVED_REVIEW_ID = 100L
    }
}
