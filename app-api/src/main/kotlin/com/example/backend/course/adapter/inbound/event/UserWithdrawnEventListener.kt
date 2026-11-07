package com.example.backend.course.adapter.inbound.event

import com.example.backend.course.application.port.inbound.UserWithdrawalCleanupUseCase
import com.example.backend.user.application.event.UserWithdrawnEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * user 의 탈퇴 이벤트를 받아 course 데이터를 정리한다.
 * 커밋 직전(BEFORE_COMMIT)에 같은 트랜잭션에서 돌아, 정리가 실패하면 탈퇴 전체가 롤백된다(예전 동기 호출과 같은 원자성).
 * user 는 course 를 모르고 사실만 알린다. 의존은 course -> user 한 방향이다.
 */
@Component
class UserWithdrawnEventListener(
    private val userWithdrawalCleanupUseCase: UserWithdrawalCleanupUseCase,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onUserWithdrawn(event: UserWithdrawnEvent) {
        userWithdrawalCleanupUseCase.cleanUpWithdrawnUser(event.userId, event.savedCourseIds)
    }
}
