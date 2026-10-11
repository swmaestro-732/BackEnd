package com.example.backend.user.application.port.outbound

/** 저장 관계 검색 색인. 모든 쓰기는 fail-soft 이며 DB 저장 결과를 바꾸지 않는다. */
interface SavedPlaceSearchIndexPort {
    fun index(document: SavedPlaceIndexDocument)

    fun index(documents: List<SavedPlaceIndexDocument>)

    fun delete(
        userId: Long,
        placeId: Long,
    )
}
