package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import org.springframework.stereotype.Component

/**
 * OS 에서 사용자·반경 내 저장 장소를 랭킹하고(거리순), 표시용 정보(이름/카테고리/이미지/평점)는 색인에 중복 저장된 값을 그대로 쓴다 — DB 왕복 없음.
 * 방문 여부(visited)만 가변 관계라 user 도메인(saved_places)에서 상위 결과만 작은 쿼리로 읽어 합친다.
 * 평점은 저장/재색인 시점 스냅샷이라 잠깐 스테일할 수 있다(실시간 신선도는 후속 Redis 트랙). 삭제된 장소는 재색인/저장 취소로 정리된다.
 */
@Component
class HomeNearbyPlaceAdapter(
    private val savedPlaceSearchQueryPort: SavedPlaceSearchQueryPort,
    private val savedPlaceUseCase: SavedPlaceUseCase,
) : HomeNearbyPlacePort {
    override fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        limit: Int,
    ): List<HomeNearbyPlace> {
        val hits = savedPlaceSearchQueryPort.nearestSaved(userId, origin, radiusMeters, limit)
        if (hits.isEmpty()) return emptyList()
        val visitedByPlace = savedPlaceUseCase.findVisited(userId, hits.map { it.placeId })
        return hits.map { hit ->
            HomeNearbyPlace(
                placeId = hit.placeId,
                name = hit.name,
                imageUrl = hit.imageUrl,
                category = hit.category,
                rating = hit.rating,
                ratingCount = hit.ratingCount,
                distanceMeters = hit.distanceMeters,
                visited = visitedByPlace[hit.placeId] ?: false,
            )
        }
    }
}
