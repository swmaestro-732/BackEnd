package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseCounterChangedEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 코스 저장·삭제·작성자 탈퇴·카운터(저장수·좋아요수) 변경 이벤트를 커밋 후(AFTER_COMMIT) 검색 인덱스에 반영한다.
 * 롤백 시엔 색인하지 않는다(고스트 문서 방지). 실패는 어댑터가 fail-soft(로그)로 삼킨다.
 * 카운터 델타는 저장·좋아요 응답 경로에서 떼어 전용 실행기([AsyncConfig.SEARCH_INDEX_COUNTER_EXECUTOR])에서 비동기로 돈다 —
 * 델타는 가환이라 순서가 바뀌어도 수렴한다. 저장·삭제는 순서에 의존하므로 아직 요청 스레드에서 동기 실행한다.
 * 서로 다른 트랜잭션(동시 요청)의 전역 순서는 보장하지 않는다 — 동시에 같은 코스를 건드리면 색인 순서가 엇갈릴 수 있다.
 * 저장·삭제의 비동기화와 대량 삭제(deleteByQuery) 분리는 SCRUM-817, 완전 순서보장·실패 재시도(outbox)는 별도 후속.
 */
@Component
class CourseSearchSyncListener(
    private val port: CourseSearchIndexPort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseSaved(event: CourseSavedEvent) {
        port.save(event.newCourse)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseDeleted(event: CourseDeletedEvent) {
        port.delete(event.courseId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseAuthorWithdrawn(event: CourseAuthorWithdrawnEvent) {
        port.deleteByAuthor(event.authorId)
    }

    @Async(AsyncConfig.SEARCH_INDEX_COUNTER_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseCounterChanged(event: CourseCounterChangedEvent) {
        port.adjustCounter(event.courseIds, event.counter, event.delta)
    }
}
