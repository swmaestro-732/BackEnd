package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 동기 색인(AFTER_COMMIT)이 실패한 저장 장소 색인 연산을 폴백 큐(SQS)로 흘려보낸다.
 * 주 경로는 동기(in-process)이고 이 포트는 실패 시 재처리 경로다(SQS 컨슈머가 같은 [SavedPlaceSearchIndexPort] 로 반영).
 * 구현(어댑터)은 큐 미설정(로컬·CI)·전송 실패를 fail-soft 로 삼킨다. upsert/delete 가 멱등이라 재전송은 안전하다.
 */
interface SavedPlaceIndexFallbackPort {
    fun enqueueIndex(document: SavedPlaceIndexDocument)

    fun enqueueDelete(
        userId: Long,
        placeId: Long,
        versionEpochMs: Long,
    )
}
