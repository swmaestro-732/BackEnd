package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.mobile.course.application.port.inbound.CourseMobileUseCase
import com.example.backend.mobile.course.application.port.inbound.CourseReviewScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseDetailScreenResult
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenQuery
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
                CourseReviewScreenQuery(courseId = courseId, size = REVIEW_PREVIEW_SIZE),
            )
        return CourseDetailScreenResult(
            course = course,
            author = author,
            places = places,
            reviewSummary = reviewSummary,
        )
    }

    private companion object {
        /** 상세 화면 리뷰 미리보기 개수 — 전체 목록은 후기 전체보기 API 가 담당한다. */
        const val REVIEW_PREVIEW_SIZE = 2
    }
}
