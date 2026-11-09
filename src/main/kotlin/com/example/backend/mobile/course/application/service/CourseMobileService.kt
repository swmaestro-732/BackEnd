package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.AdminCourseUseCase
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CoursePreviewCommand
import com.example.backend.mobile.course.application.port.inbound.CourseMobileUseCase
import com.example.backend.mobile.course.application.port.inbound.CourseReviewScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenQuery
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 코스 상세 화면 조합 서비스 (BFF). */
@Service
@Transactional(readOnly = true)
class CourseMobileService(
    private val courseQueryUseCase: CourseQueryUseCase,
    private val userUseCase: UserUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val courseReviewScreenUseCase: CourseReviewScreenUseCase,
    private val adminCourseUseCase: AdminCourseUseCase,
) : CourseMobileUseCase {
    override fun getScreen(
        courseId: Long,
        viewerId: Long?,
    ): CourseDetailScreenResult {
        val course = courseQueryUseCase.getDetail(courseId, viewerId)
        val author = userUseCase.getProfile(course.authorId, viewerId)
        val places = placeQueryUseCase.findPlacesById(course.places.map { it.placeId })
        val reviewSummary =
            courseReviewScreenUseCase.getScreen(
                CourseReviewScreenQuery(courseId = courseId, viewerId = viewerId, size = REVIEW_PREVIEW_SIZE),
            )
        return CourseDetailScreenResult(
            course = course,
            author = author,
            places = places,
            reviewSummary = reviewSummary,
        )
    }

    /**
     * 미리보기는 어드민 코스 포트가 실제 상세와 같은 규칙으로 계산한 코스에 작성자와 장소를 붙인다.
     * 조회자가 없으니 팔로우 관계와 viewer 플래그는 false, 리뷰 요약은 빈 값이다(읽기 전용 트랜잭션).
     */
    override fun getPreviewScreen(command: CoursePreviewCommand): CourseDetailScreenResult {
        val course = adminCourseUseCase.preview(command)
        return CourseDetailScreenResult(
            course = course,
            author = userUseCase.getProfile(course.authorId, null),
            places = placeQueryUseCase.findPlacesById(course.places.map { it.placeId }.distinct()),
            reviewSummary =
                CourseReviewScreenResult(
                    averageRating = 0.0,
                    totalCount = 0,
                    ratingDistribution = (5 downTo 1).map { CourseReviewScreenResult.RatingCount(it, 0) },
                    photoCount = 0,
                    hasCompletedCourse = false,
                    nextCursor = null,
                    hasNext = false,
                    reviews = emptyList(),
                ),
        )
    }

    private companion object {
        /** 상세 화면 리뷰 미리보기 개수 — 전체 목록은 후기 전체보기 API 가 담당한다. */
        const val REVIEW_PREVIEW_SIZE = 2
    }
}
