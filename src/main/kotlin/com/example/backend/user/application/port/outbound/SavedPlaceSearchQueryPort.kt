package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.outbound.dto.NearbySavedHit

/** 홈에 공개하는 저장 장소 거리 조회 계약. 검색엔진 미설정·장애는 빈 목록으로 대체한다. */
interface SavedPlaceSearchQueryPort {
    fun nearestSaved(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        size: Int,
    ): List<NearbySavedHit>
}
