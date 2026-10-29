package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import org.springframework.stereotype.Component

/**
 * OS 에서 사용자와 반경 내 저장 장소를 거리순으로 조회한다.
 * 표시용 정보(이름/카테고리/이미지/평점)와 방문 여부(visited) 모두 색인에서 읽어 DB 왕복을 완전히 제거한다.
 * 방문 기능 구현 시 DB 변경과 함께 색인 visited 동기화가 필요하다.
 * 평점은 저장/재색인 시점 스냅샷이라 잠깐 스테일할 수 있다(실시간 신선도는 후속 Redis 트랙). 삭제된 장소는 재색인/저장 취소로 정리된다.
 */
@Component
class HomeNearbyPlaceAdapter(
    private val savedPlaceUseCase: SavedPlaceUseCase,
) : HomeNearbyPlacePort {
    override fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        limit: Int,
    ): List<HomeNearbyPlace> {
        val hits = savedPlaceUseCase.nearestSaved(userId, origin, radiusMeters, limit)
        return hits.map { hit ->
            HomeNearbyPlace(
                placeId = hit.placeId,
                name = hit.name,
                imageUrl = hit.imageUrl,
                category = hit.category,
                rating = hit.rating,
                ratingCount = hit.ratingCount,
                distanceMeters = hit.distanceMeters,
                visited = hit.visited,
            )
        }
    }
}
