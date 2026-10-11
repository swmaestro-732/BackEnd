package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.outbound.SavedPlaceSearchQueryPort
import org.springframework.stereotype.Component

/** OS 에서 사용자·반경 내 저장 장소를 랭킹하고, 결과 ID 만 DB 에서 조회해 최신 상세를 합친다. */
@Component
class HomeNearbyPlaceAdapter(
    private val savedPlaceSearchQueryPort: SavedPlaceSearchQueryPort,
    private val placeQueryUseCase: PlaceQueryUseCase,
) : HomeNearbyPlacePort {
    override fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        radiusMeters: Double,
        limit: Int,
    ): List<HomeNearbyPlace> {
        val hits = savedPlaceSearchQueryPort.nearestSaved(userId, origin, radiusMeters, limit)
        if (hits.isEmpty()) return emptyList()
        val places = placeQueryUseCase.findPlacesById(hits.map { it.placeId }).associateBy { it.id }
        // DB 조회 순서와 무관하게 OS 거리순을 보존한다. DB 에서 삭제된 장소는 제외한다.
        return hits.mapNotNull { hit ->
            places[hit.placeId]?.let { place ->
                HomeNearbyPlace(
                    placeId = place.id,
                    name = place.name,
                    imageUrl = place.imageUrl,
                    category = place.category,
                    rating = place.rating,
                    ratingCount = place.ratingCount,
                    distanceMeters = hit.distanceMeters,
                    visited = hit.visited,
                )
            }
        }
    }
}
