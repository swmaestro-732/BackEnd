package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.TracingRepository
import com.example.backend.user.application.port.outbound.TracingPersistencePort
import com.example.backend.user.application.port.outbound.TracingRow
import org.springframework.stereotype.Component
import java.time.Instant

/** 아웃바운드 어댑터 — [TracingPersistencePort] 를 구현한다. 실제 테이블 접근은 [TracingRepository] 에 위임한다. */
@Component
class TracingPersistenceAdapter(
    private val tracingRepository: TracingRepository,
) : TracingPersistencePort {
    override fun findActiveByUserCourse(
        userId: Long,
        courseId: Long,
    ): Long? = tracingRepository.findActiveByUserCourse(userId, courseId)

    override fun insertTracing(
        userId: Long,
        courseId: Long,
    ): Long = tracingRepository.insertTracing(userId, courseId)

    override fun findOwned(
        userId: Long,
        tracingId: Long,
    ): TracingRow? = tracingRepository.findOwned(userId, tracingId)

    override fun checkInPlace(
        tracingId: Long,
        placeId: Long,
    ) = tracingRepository.checkInPlace(tracingId, placeId)

    override fun countCheckedPlaces(tracingId: Long): Int = tracingRepository.countCheckedPlaces(tracingId)

    override fun markCompleted(
        tracingId: Long,
        at: Instant,
    ) = tracingRepository.markCompleted(tracingId, at)
}
