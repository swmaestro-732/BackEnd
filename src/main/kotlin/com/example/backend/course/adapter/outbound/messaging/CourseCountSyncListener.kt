package com.example.backend.course.adapter.outbound.messaging

import com.example.backend.course.application.event.CourseCountDeltaEvent
import com.example.backend.course.application.port.outbound.CourseCountMessagePort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 코스 개수 델타 이벤트를 커밋 후(AFTER_COMMIT) SQS 로 발행한다([CourseSearchSyncListener] 와 동일한 패턴).
 * 롤백된 트랜잭션은 이벤트를 흘리지 않는다 — 반영되지 않은 변경으로 카운터가 어긋나는 것을 막는다.
 * 발행 실패는 어댑터가 fail-soft(warn)로 삼킨다.
 */
@Component
class CourseCountSyncListener(
    private val courseCountMessagePort: CourseCountMessagePort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseCountDelta(event: CourseCountDeltaEvent) {
        courseCountMessagePort.send(event)
    }
}
