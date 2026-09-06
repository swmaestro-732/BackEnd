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
}
