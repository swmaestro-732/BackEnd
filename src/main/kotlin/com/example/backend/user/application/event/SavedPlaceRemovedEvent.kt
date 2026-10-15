package com.example.backend.user.application.event

import java.time.Instant

/** 저장 취소 후 동일한 복합 키의 검색 문서를 삭제한다. [removedAt]은 external version(순서 역전 방지)으로 쓴다. */
data class SavedPlaceRemovedEvent(
    val userId: Long,
    val placeId: Long,
    val removedAt: Instant,
)
