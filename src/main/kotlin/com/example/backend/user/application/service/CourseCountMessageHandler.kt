package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.CourseCountMessageUseCase
import com.example.backend.user.application.port.inbound.UserCourseCountUseCase
import com.example.backend.user.application.port.outbound.ProcessedCourseCountEventPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 개수 델타 메시지 처리기(user 도메인 소유) — SQS 폴러가 수신한 메시지를 멱등하게 반영한다.
 *
 * 표준 큐는 at-least-once 라 같은 메시지가 재전송될 수 있다 → eventId 를 처리 이력에 먼저 기록(insert-if-absent)해
 * 이미 처리된 것이면 건너뛴다(멱등). 이력 기록과 델타 반영을 한 트랜잭션으로 묶어, 반영 전 크래시 시 둘 다 롤백돼
 * 재전송으로 다시 처리되게 한다(=정확히 한 번 반영). 카운터 반영은 user 자신의 인바운드 포트로 한다.
 */
@Service
class CourseCountMessageHandler(
    private val processedEventPort: ProcessedCourseCountEventPort,
    private val userCourseCountUseCase: UserCourseCountUseCase,
) : CourseCountMessageUseCase {
    @Transactional
    override fun handle(
        eventId: String,
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    ) {
        // 이미 처리한 메시지면(재전송) 아무 것도 하지 않는다 — 델타 중복 반영 방지.
        if (!processedEventPort.markProcessedIfAbsent(eventId)) return
        userCourseCountUseCase.applyCourseCountDelta(userId, publicDelta, followerDelta, privateDelta)
    }
}
