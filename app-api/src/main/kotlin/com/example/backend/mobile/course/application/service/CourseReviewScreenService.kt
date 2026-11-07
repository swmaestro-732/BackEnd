package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseReviewQueryUseCase
import com.example.backend.course.application.port.inbound.CourseTraceUseCase
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
    private val courseTraceUseCase: CourseTraceUseCase,
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
        // 비로그인은 완주 이력이 있을 수 없으니 false. 로그인 유저는 이 코스를 완주(COMPLETED 세션)한 적이 있는지로 판정한다.
        val hasCompletedCourse =
            query.viewerId?.let { viewerId ->
                query.courseId in courseTraceUseCase.findCompletedAt(viewerId, listOf(query.courseId))
            } ?: false
        return CourseReviewScreenResult.of(page, authors, hasCompletedCourse = hasCompletedCourse)
    }
}
