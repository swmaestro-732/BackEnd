package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate
import java.time.Instant

/** 저장 관계와 장소 좌표의 색인용 스냅샷. 검색엔진 타입은 어댑터 안에서만 사용한다. */
data class SavedPlaceIndexDocument(
    val userId: Long,
    val placeId: Long,
    val location: Coordinate,
    val visited: Boolean,
    val savedAt: Instant,
)
