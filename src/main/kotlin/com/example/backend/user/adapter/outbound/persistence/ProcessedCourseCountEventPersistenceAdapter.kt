package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.ProcessedCourseCountEventRepository
import com.example.backend.user.application.port.outbound.ProcessedCourseCountEventPort
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [ProcessedCourseCountEventPort] 를 구현한다.
 * 실제 테이블 접근은 [ProcessedCourseCountEventRepository] 에 위임한다.
 */
@Component
class ProcessedCourseCountEventPersistenceAdapter(
    private val repository: ProcessedCourseCountEventRepository,
) : ProcessedCourseCountEventPort {
    override fun markProcessedIfAbsent(eventId: String): Boolean = repository.markProcessedIfAbsent(eventId)
}
