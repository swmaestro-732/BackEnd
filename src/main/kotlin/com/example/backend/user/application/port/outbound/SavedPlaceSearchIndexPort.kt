package com.example.backend.user.application.port.outbound

/**
 * 저장 관계 검색 색인. DB 저장 결과를 바꾸지 않는다.
 * 단건 [index](document)/[delete] 는 전송 실패를 던지고(호출자가 SQS 폴백으로 재처리), 벌크 [index](documents) 는 재색인 best-effort 라 삼킨다.
 */
interface SavedPlaceSearchIndexPort {
    /** 단건 동기 색인 — 실패 시 예외를 던진다(리스너가 폴백 큐로 재처리). */
    fun index(document: SavedPlaceIndexDocument)

    /** 벌크 재색인(backfill) — best-effort 라 실패를 삼킨다. */
    fun index(documents: List<SavedPlaceIndexDocument>)

    fun delete(
        userId: Long,
        placeId: Long,
    )
}
