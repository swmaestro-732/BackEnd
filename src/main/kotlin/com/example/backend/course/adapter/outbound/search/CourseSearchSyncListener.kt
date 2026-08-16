package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 코스 저장·삭제·작성자 탈퇴 이벤트를 커밋 후(AFTER_COMMIT) 비동기로 검색 인덱스에 반영한다.
 * 요청/트랜잭션 스레드를 막지 않고, 롤백 시엔 색인하지 않는다(고스트 문서 방지).
 */
@Component
class CourseSearchSyncListener(
    private val port: CourseSearchIndexPort,
) {
    @Async("searchIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseSaved(event: CourseSavedEvent) {
        // TODO(SQS): 실패 시 재처리 큐로 폴백 — 현재는 어댑터가 fail-soft(warn) 처리.
        port.save(event.course)
    }

    @Async("searchIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseDeleted(event: CourseDeletedEvent) {
        // TODO(SQS): 실패 시 재처리 큐로 폴백 — 현재는 어댑터가 fail-soft(warn) 처리.
        port.delete(event.courseId)
    }

    @Async("searchIndexExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseAuthorWithdrawn(event: CourseAuthorWithdrawnEvent) {
        // TODO(SQS): 실패 시 재처리 큐로 폴백 — 현재는 어댑터가 fail-soft(warn) 처리.
        port.deleteByAuthor(event.authorId)
    }
}
