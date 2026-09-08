package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseReviewQueryRepository
import com.example.backend.course.application.port.inbound.dto.CourseReviewSortKey
import com.example.backend.course.application.port.outbound.CourseRatingCounters
import com.example.backend.course.application.port.outbound.CourseReviewCursor
import com.example.backend.course.application.port.outbound.CourseReviewQueryPort
import com.example.backend.course.application.port.outbound.CourseReviewRow
import com.example.backend.course.domain.model.CourseReviewTag
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseReviewQueryPort] 를 구현한다.
 * 실제 테이블 접근은 [CourseReviewQueryRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class CourseReviewQueryAdapter(
    private val courseReviewQueryRepository: CourseReviewQueryRepository,
) : CourseReviewQueryPort {
    override fun findReviewsByCourse(
        courseId: Long,
        sort: CourseReviewSortKey,
        descending: Boolean,
        cursor: CourseReviewCursor?,
        limit: Int,
    ): List<CourseReviewRow> =
        courseReviewQueryRepository.findReviewsByCourse(courseId, sort, descending, cursor, limit)

    override fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>> =
        courseReviewQueryRepository.findPhotoUrls(reviewIds)

    override fun findTags(reviewIds: List<Long>): Map<Long, List<CourseReviewTag>> =
        courseReviewQueryRepository.findTags(reviewIds)

    override fun countReviewsByRating(courseId: Long): Map<Int, Long> =
        courseReviewQueryRepository.countReviewsByRating(courseId)

    override fun countPhotosByCourse(courseId: Long): Long = courseReviewQueryRepository.countPhotosByCourse(courseId)

    override fun findRatingCounters(courseId: Long): CourseRatingCounters? =
        courseReviewQueryRepository.findRatingCounters(courseId)
}
