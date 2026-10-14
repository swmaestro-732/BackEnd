package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.SavedPlaceSearchQueryPort
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import org.springframework.stereotype.Component

/**
 * OS 에서 사용자·반경 내 저장 장소를 랭킹하고(거리순), 결과 ID 만 DB 에서 조회해 최신 상세를 합친다.
 * 장소 상세(이름/평점)는 place 도메인에서, 방문 여부는 user 도메인(saved_places)에서 신선하게 읽는다 — 둘 다 상위 결과만.
 */
@Component
class HomeNearbyPlaceAdapter(
    private val savedPlaceSearchQueryPort: SavedPlaceSearchQueryPort,
    private val savedPlaceUseCase: SavedPlaceUseCase,
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
        val placeIds = hits.map { it.placeId }
        val places = placeQueryUseCase.findPlacesById(placeIds).associateBy { it.id }
        val visitedByPlace = savedPlaceUseCase.findVisited(userId, placeIds)
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
                    visited = visitedByPlace[place.id] ?: false,
                )
            }
        }
    }
}
