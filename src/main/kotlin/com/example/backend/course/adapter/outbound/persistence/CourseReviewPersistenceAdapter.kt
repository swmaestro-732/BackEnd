package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseReviewRepository
import com.example.backend.course.application.port.outbound.CourseReviewPersistencePort
import com.example.backend.course.domain.model.CourseReview
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseReviewPersistencePort] 를 구현한다.
 */
@Component
class CourseReviewPersistenceAdapter(
    private val courseReviewRepository: CourseReviewRepository,
) : CourseReviewPersistencePort {
    override fun save(review: CourseReview): CourseReview = courseReviewRepository.insert(review)
}
