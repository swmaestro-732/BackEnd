package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 코스 저장·삭제·작성자 탈퇴 이벤트를 커밋 후(AFTER_COMMIT) 검색 인덱스에 동기 반영한다.
 * 롤백 시엔 색인하지 않는다(고스트 문서 방지). 요청 스레드에서 단건 upsert/delete 라 가볍고,
 * 실패는 어댑터가 fail-soft(warn)로 삼킨다.
 * 단, 서로 다른 트랜잭션(동시 요청)의 전역 순서는 보장하지 않는다 — 동시에 같은 코스를 건드리면
 * 색인 순서가 엇갈릴 수 있다. 완전 순서보장(버전/시퀀스 검사)·응답 지연 최적화·실패 재시도·
 * 대량 삭제(deleteByQuery) 분리는 outbox/큐 후속에서 다룬다 — SCRUM-483.
 */
@Component
class CourseSearchSyncListener(
    private val port: CourseSearchIndexPort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseSaved(event: CourseSavedEvent) {
        port.save(event.course)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseDeleted(event: CourseDeletedEvent) {
        port.delete(event.courseId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseAuthorWithdrawn(event: CourseAuthorWithdrawnEvent) {
        port.deleteByAuthor(event.authorId)
    }
}
