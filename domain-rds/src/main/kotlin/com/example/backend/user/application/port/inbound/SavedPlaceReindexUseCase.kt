package com.example.backend.user.application.port.inbound

interface SavedPlaceReindexUseCase {
    /** 살아있는 저장 관계를 배치로 upsert 하고 색인을 시도한 문서 수를 반환한다. */
    fun reindexAll(): Int
}
