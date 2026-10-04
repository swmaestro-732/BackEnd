package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.ProcessedCourseCountEventTable
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.springframework.stereotype.Repository

/** processed_course_count_events 접근 리포지토리 — 메시지 멱등 처리를 위한 event_id 이력 기록. */
@Repository
class ProcessedCourseCountEventRepository {
    /**
     * event_id 를 처리 이력에 기록한다(있으면 무시). 새로 기록됐으면 true, 이미 있었으면 false.
     * PK(event_id) 충돌을 insertIgnore(= ON CONFLICT DO NOTHING)로 흡수해 멱등을 보장한다.
     */
    fun markProcessedIfAbsent(eventId: String): Boolean =
        ProcessedCourseCountEventTable
            .insertIgnore {
                it[ProcessedCourseCountEventTable.eventId] = eventId
            }.insertedCount > 0
}
