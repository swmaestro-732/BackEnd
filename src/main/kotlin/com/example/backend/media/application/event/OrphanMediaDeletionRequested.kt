package com.example.backend.media.application.event

/**
 * 참조가 끊긴 미디어(S3 key) 삭제 요청 이벤트.
 *
 * 트랜잭션 안에서 발행되면 커밋 이후에만 처리된다([OrphanMediaDeletionListener]) — 롤백 시 삭제되지 않아
 * DB 상태와 S3 의 정합성이 깨지지 않는다.
 */
data class OrphanMediaDeletionRequested(
    val key: String,
)
