package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.course.application.port.inbound.AuthorCourseCountChanged
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

/**
 * user 도메인 ACL — course 의 공개범위 전이 이벤트([AuthorCourseCountChanged] 공개 계약)를 받아 작성자 코스 개수에 반영한다.
 * 크로스도메인 결합을 어댑터에 격리하고 course 내부 이벤트가 아니라 **inbound 포트 계약만** 참조한다(규칙 7).
 *
 * 주 경로는 **동기(in-process)**: 커밋 후(AFTER_COMMIT) [CourseCountUseCase.apply] 를 바로 호출한다.
 * 실패하면 그때만 [CourseCountFallbackPort] 로 SQS 폴백에 실어 재시도한다(SQS 컨슈머가 같은 use case 로 반영).
 * eventId 를 한 번 만들어 동기·폴백에 공유하고 멱등은 use case 안에서 지켜 재전송·이중 집계를 막는다.
 */
@Component
class CourseCountEventHandler(
    private val courseCountUseCase: CourseCountUseCase,
    private val fallbackPort: CourseCountFallbackPort,
) {
    private val log = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseCountChanged(event: AuthorCourseCountChanged) {
        val eventId = UUID.randomUUID().toString()
        try {
            courseCountUseCase.apply(eventId, event.authorId, event.oldVisibility, event.newVisibility) // 동기 우선
        } catch (e: Exception) {
            log.warn { "동기 코스 개수 반영 실패 → SQS 폴백: authorId=${event.authorId} eventId=$eventId — ${e.message}" }
            fallbackPort.publish(event.authorId, event.oldVisibility, event.newVisibility, eventId) // 실패 시 폴백
        }
    }
}
