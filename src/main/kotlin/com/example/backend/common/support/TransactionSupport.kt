package com.example.backend.common.support

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * 현재 트랜잭션이 성공적으로 커밋된 뒤 [action] 을 실행한다. 트랜잭션 밖이면 즉시 실행.
 *
 * 커밋 이후에만 해야 하는 외부 부작용(예: S3 객체 삭제)에 쓴다 — 롤백되면 실행되지 않아
 * DB 상태와 외부 리소스의 정합성이 깨지지 않는다.
 */
fun runAfterCommit(action: () -> Unit) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        action()
        return
    }
    TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
            override fun afterCommit() = action()
        },
    )
}
