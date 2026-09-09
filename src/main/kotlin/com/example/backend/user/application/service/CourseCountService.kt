package com.example.backend.user.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.application.port.inbound.CourseCountUseCase
import com.example.backend.user.application.port.outbound.CourseCountPersistencePort
import com.example.backend.user.application.port.outbound.ProcessedCourseCountEventPort
import com.example.backend.user.domain.model.CourseCountDelta
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 공개범위 전이 → 작성자 개수 버킷 반영(user 도메인 소유). 동기 ACL 핸들러·SQS 폴백 컨슈머 공용.
 *
 * 규칙(델타)은 도메인 [CourseCountDelta] 가, 멱등은 이 서비스가 맡는다:
 * old==new 면 no-op, 아니면 eventId 를 처리 이력에 먼저 기록(insert-if-absent)해 이미 처리한 재전송은 건너뛴다.
 * 이력 기록과 카운트 반영을 한 트랜잭션으로 묶어, 반영 전 크래시 시 둘 다 롤백돼 재전송으로 다시 처리된다
 * (=정확히 한 번 반영). 표준 큐(at-least-once) 재전송·동기 실패 후 폴백 재전달 모두 이 멱등으로 안전하다.
 *
 * 동기 경로는 [CourseSavedEvent] 를 커밋 후(AFTER_COMMIT) 받는 ACL 핸들러가 호출한다 — 그 시점엔 원 트랜잭션이
 * 이미 커밋·종료됐으므로 REQUIRED 로는 새 트랜잭션이 열리지 않아 쓰기가 유실될 수 있다. 그래서 **REQUIRES_NEW** 로
 * 항상 독립 트랜잭션을 연다(dedup insert + 카운트 반영을 원자적으로 커밋). SQS 폴백 경로엔 감싸는 트랜잭션이 없어 무해하다.
 */
@Service
class CourseCountService(
    private val processedEventPort: ProcessedCourseCountEventPort,
    private val courseCountPersistencePort: CourseCountPersistencePort,
) : CourseCountUseCase {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun apply(
        eventId: String,
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
    ) {
        val delta = CourseCountDelta.of(oldVisibility, newVisibility)
        if (delta.isNoop()) return // 카운트되는 공개범위 변화 없음 — 이력도 남기지 않는다.

        // 이미 처리한 메시지면(재전송) 아무 것도 하지 않는다 — 중복 반영 방지.
        if (!processedEventPort.markProcessedIfAbsent(eventId)) return

        courseCountPersistencePort.applyCourseCountDelta(
            userId = authorId,
            publicDelta = delta.publicDelta,
            followerDelta = delta.followerDelta,
            privateDelta = delta.privateDelta,
        )
    }
}
