package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.SavedCourseRepository
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.TracingCourseRepository
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 아웃바운드 어댑터 — [CourseInteractionPort] 를 구현한다.
 * 완주 시각은 [TracingCourseRepository], 저장 여부는 [SavedCourseRepository] 에 위임해
 * (user_id, course_id) 기준으로 배치 조회한다.
 */
@Component
class CourseInteractionAdapter(
    private val tracingCourseRepository: TracingCourseRepository,
    private val savedCourseRepository: SavedCourseRepository,
) : CourseInteractionPort {
    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = tracingCourseRepository.findCompletedAt(userId, courseIds)

    override fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> = savedCourseRepository.findSavedCourseIds(userId, courseIds)
}
