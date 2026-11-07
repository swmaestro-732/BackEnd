package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseLikeRepository
import com.example.backend.course.application.port.outbound.CourseLikePersistencePort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [CourseLikePersistencePort] 를 구현한다. 테이블 접근은 [CourseLikeRepository] 에 위임한다. */
@Component
class CourseLikePersistenceAdapter(
    private val courseLikeRepository: CourseLikeRepository,
) : CourseLikePersistencePort {
    override fun existsLike(
        userId: Long,
        courseId: Long,
    ): Boolean = courseLikeRepository.existsLike(userId, courseId)

    override fun insert(
        userId: Long,
        courseId: Long,
    ) = courseLikeRepository.insert(userId, courseId)

    override fun deleteByUserAndCourse(
        userId: Long,
        courseId: Long,
    ): Boolean = courseLikeRepository.deleteByUserAndCourse(userId, courseId)

    override fun deleteAllByUser(userId: Long): List<Long> = courseLikeRepository.deleteAllByUser(userId)
}
