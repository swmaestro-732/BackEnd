package com.example.backend.user.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.dto.NearbySavedHit

/**
 * 서비스가 사용하는 저장 장소 거리 조회 아웃바운드 포트.
 * BFF는 인바운드 [com.example.backend.user.application.port.inbound.SavedPlaceUseCase]를 통해 조회한다.
 * 검색엔진 미설정, 장애는 빈 목록으로 대체한다.
 */
interface SavedPlaceSearchQueryPort {
    fun nearestSaved(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        size: Int,
    ): List<NearbySavedHit>
}
