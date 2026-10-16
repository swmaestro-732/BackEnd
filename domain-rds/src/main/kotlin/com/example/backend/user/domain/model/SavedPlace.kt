package com.example.backend.user.domain.model

import java.time.Instant

/**
 * 저장 장소 — 사용자가 저장한 장소 레코드(saved_places).
 * placeId 는 cross-domain(place) 참조라 FK 없이 id 만 들고 있고,
 * category 는 저장 시점 place 카테고리 스냅샷([SavedPlaceCategory], 미분류면 null)이다.
 */
data class SavedPlace(
    val id: Long,
    val userId: Long,
    val placeId: Long,
    val category: SavedPlaceCategory?,
    val visited: Boolean,
    val savedAt: Instant,
    val deletedAt: Instant? = null, // 소프트 삭제 시각, 살아있으면 null
)
