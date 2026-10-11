package com.example.backend.user.application.event

import com.example.backend.common.geo.Coordinate
import java.time.Instant

/** 커밋 후 색인할 저장 관계 스냅샷. 롤백 시에는 소비하지 않는다. */
data class SavedPlaceIndexedEvent(
    val userId: Long,
    val placeId: Long,
    val location: Coordinate,
    val visited: Boolean,
    val savedAt: Instant,
)
