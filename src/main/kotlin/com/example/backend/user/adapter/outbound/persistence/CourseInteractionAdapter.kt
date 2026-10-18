package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.SavedCourseRepository
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [CourseInteractionPort] 를 구현한다. 저장 여부는 [SavedCourseRepository] 에 위임한다. */
@Component
class CourseInteractionAdapter(
    private val savedCourseRepository: SavedCourseRepository,
) : CourseInteractionPort {
    override fun findSavedCourseIds(
        userId: Long,
        courseIds: List<Long>,
    ): Set<Long> = savedCourseRepository.findSavedCourseIds(userId, courseIds)
}
