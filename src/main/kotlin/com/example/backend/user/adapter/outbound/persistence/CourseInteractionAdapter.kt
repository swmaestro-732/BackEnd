package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.CourseInteractionRepository
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [CourseInteractionPort] 를 구현한다. 실제 테이블 접근은 [CourseInteractionRepository] 에 위임한다. */
@Component
class CourseInteractionAdapter(
    private val courseInteractionRepository: CourseInteractionRepository,
) : CourseInteractionPort {
    override fun existsSavedCourse(
        userId: Long,
        courseId: Long,
    ): Boolean = courseInteractionRepository.existsSavedCourse(userId, courseId)

    override fun existsTracingCourse(
        userId: Long,
        courseId: Long,
    ): Boolean = courseInteractionRepository.existsTracingCourse(userId, courseId)

    override fun countSavesByCourseIds(courseIds: List<Long>): Map<Long, Int> =
        courseInteractionRepository.countSavesByCourseIds(courseIds)
}
