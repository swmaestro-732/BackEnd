package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.CourseReviewUseCase
import com.example.backend.course.application.port.inbound.dto.CreateCourseReviewCommand
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.CourseReviewPersistencePort
import com.example.backend.course.domain.model.CourseReview
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 코스 리뷰 작성·삭제 유스케이스. */
@Service
@Transactional
class CourseReviewService(
    private val coursePersistencePort: CoursePersistencePort,
    private val courseReviewPersistencePort: CourseReviewPersistencePort,
) : CourseReviewUseCase {
    override fun create(command: CreateCourseReviewCommand): CourseReview {
        requireCourseExist(command.courseId)
        requireNoExistingReview(command.courseId, command.userId)

        return courseReviewPersistencePort.save(
            CourseReview.create(
                courseId = command.courseId,
                userId = command.userId,
                rating = command.rating,
                content = command.content,
                photoUrls = command.photoUrls,
                tags = command.tags,
            ),
        )
    }

    override fun delete(
        userId: Long,
        courseId: Long,
        reviewId: Long,
    ) {
        val deleted = courseReviewPersistencePort.softDelete(reviewId = reviewId, courseId = courseId, userId = userId)
        if (deleted == 0) {
            throw BusinessException(CourseErrorCode.COURSE_REVIEW_NOT_FOUND)
        }
    }

    private fun requireCourseExist(courseId: Long) {
        if (!coursePersistencePort.existsById(courseId)) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
    }

    /**
     * 코스 하나에 사용자당 리뷰는 1개 — 이미 있으면 409. 삭제한 리뷰는 세지 않아 다시 쓸 수 있다.
     * 동시 작성 경합은 이 검사를 통과할 수 있어 DB 유니크 인덱스(uq_course_reviews_user_course)가 최종 방어선이다.
     */
    private fun requireNoExistingReview(
        courseId: Long,
        userId: Long,
    ) {
        if (courseReviewPersistencePort.existsActiveReview(courseId = courseId, userId = userId)) {
            throw BusinessException(
                CourseErrorCode.COURSE_REVIEW_ALREADY_EXISTS,
                "이미 이 코스에 리뷰를 작성했습니다: courseId=$courseId",
            )
        }
    }
}
