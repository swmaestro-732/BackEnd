package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTraceRepository
import com.example.backend.course.application.port.inbound.dto.EndCourseTraceCommand
import com.example.backend.course.application.port.outbound.CourseTracePersistencePort
import org.springframework.stereotype.Component
import java.time.Instant

/** 아웃바운드 어댑터 — [CourseTracePersistencePort] 를 구현한다. 테이블 접근은 [CourseTraceRepository] 에 위임한다. */
@Component
class CourseTracePersistenceAdapter(
    private val courseTraceRepository: CourseTraceRepository,
) : CourseTracePersistencePort {
    override fun insert(command: EndCourseTraceCommand) = courseTraceRepository.insert(command)

    override fun findCompletedAt(
        userId: Long,
        courseIds: List<Long>,
    ): Map<Long, Instant> = courseTraceRepository.findCompletedAt(userId, courseIds)

    override fun findTracedCourseIds(userId: Long): Set<Long> = courseTraceRepository.findTracedCourseIds(userId)
}
