package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseReviewQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseReviewsQuery
import com.example.backend.mobile.course.application.port.inbound.CourseReviewScreenUseCase
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenQuery
import com.example.backend.mobile.course.application.port.inbound.dto.CourseReviewScreenResult
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 코스 후기 전체보기 화면 조합 서비스(BFF). */
@Service
@Transactional(readOnly = true)
class CourseReviewScreenService(
    private val courseReviewQueryUseCase: CourseReviewQueryUseCase,
    private val userSummaryUseCase: UserSummaryUseCase,
) : CourseReviewScreenUseCase {
    override fun getScreen(query: CourseReviewScreenQuery): CourseReviewScreenResult {
        val page =
            courseReviewQueryUseCase.getReviews(
                CourseReviewsQuery(
                    courseId = query.courseId,
                    sort = query.sort,
                    descending = query.descending,
                    cursor = query.cursor,
                    size = query.size,
                ),
            )
        val authors =
            userSummaryUseCase
                .findSummaries(page.reviews.map { it.userId }.distinct())
                .associateBy { it.id }
        return CourseReviewScreenResult.of(page, authors, hasCompletedCourse = STUB_HAS_COMPLETED_COURSE)
    }

    companion object {
        /** STUB: 완주(따라가기 완료) 판정 전 고정값(모킹 응답과 같은 값). tracing_courses 실구현 시 제거한다. */
        const val STUB_HAS_COMPLETED_COURSE = true
    }
}
