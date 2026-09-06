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

/**
 * 코스 리뷰 작성 유스케이스.
 *
 * 리뷰 대상 코스가 살아 있는지 [CoursePersistencePort.existsById] 로 확인한다(없거나 삭제됐으면 404).
 * 태그 코드 → 도메인 enum 변환(모르는 코드는 400)은 웹 어댑터(toCommand)가 맡는다.
 * 별점·사진 개수·한마디 길이 같은 불변식은 [CourseReview.create] 가 검증한다.
 * 리뷰 본문과 사진·태그 연결은 [CourseReviewPersistencePort.save] 가 한 트랜잭션에 함께 심는다.
 */
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

    private fun requireCourseExist(courseId: Long) {
        if (!coursePersistencePort.existsById(courseId)) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND)
        }
    }
}
