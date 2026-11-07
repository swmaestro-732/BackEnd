package com.example.backend.user.application.event

/** 저장 취소 후 동일한 복합 키의 검색 문서를 삭제한다. */
data class SavedPlaceRemovedEvent(
    val userId: Long,
    val placeId: Long,
)
