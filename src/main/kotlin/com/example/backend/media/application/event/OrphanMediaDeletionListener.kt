package com.example.backend.media.application.event

import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * [OrphanMediaDeletionRequested] 를 받아 실제 S3 삭제를 수행한다.
 *
 * 트랜잭션 커밋 이후(AFTER_COMMIT)에만 실행하고, 트랜잭션 밖에서 발행되면
 * `fallbackExecution = true` 로 즉시 실행한다. 삭제 자체는 fail-soft(어댑터에서 예외 무시).
 */
@Component
class OrphanMediaDeletionListener(
    private val mediaStoragePort: MediaStoragePort,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handle(event: OrphanMediaDeletionRequested) {
        mediaStoragePort.delete(event.key)
    }
}
