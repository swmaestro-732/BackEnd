package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument

interface SavedPlaceReindexUseCase {
    /** 살아있는 저장 관계를 배치로 upsert 하고 색인을 시도한 문서 수를 반환한다. */
    fun reindexAll(): Int

    /**
     * (userId, placeId) 의 현재 DB 상태로 만든 색인 문서. 저장돼 있지 않거나 장소가 없으면 null(색인에서 지울 대상).
     * 저장 장소 색인 동기화가 이벤트 순서와 재발행에 상관없이 DB 와 같아지도록 매번 다시 읽는다.
     */
    fun findDocument(
        userId: Long,
        placeId: Long,
    ): SavedPlaceIndexDocument?
}
