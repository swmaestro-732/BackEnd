package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.CourseInteractionRepository
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.springframework.stereotype.Component
import java.time.Instant

/** 아웃바운드 어댑터 — [CourseInteractionPort] 를 구현한다. 실제 테이블 접근은 [CourseInteractionRepository] 에 위임한다. */
@Component
class CourseInteractionAdapter(
    private val courseInteractionRepository: CourseInteractionRepository,
) : CourseInteractionPort {
    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = courseInteractionRepository.findCompletedAt(userId, courseIds)

    override fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> = courseInteractionRepository.findSavedCourseIds(userId, courseIds)
}
