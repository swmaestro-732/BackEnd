package com.example.backend.user.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.application.port.inbound.dto.NearbySavedHit

/**
 * 홈에 공개하는 저장 장소 거리 조회 계약. 검색엔진 미설정, 장애는 빈 목록으로 대체한다.
 *
 * 구현은 검색엔진(OS)에 질의하지만 포트는 inbound 에 둔다 — 홈 BFF 가 이 포트를 참조하는데
 * ArchUnit 규칙("BFF 는 도메인의 inbound 포트만 참조한다")을 지키려면 inbound 여야 하기 때문이다.
 * outbound 로 옮기지 말 것(HexagonalArchitectureTest 가 깨진다).
 */
interface SavedPlaceSearchQueryPort {
    fun nearestSaved(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        size: Int,
    ): List<NearbySavedHit>
}
