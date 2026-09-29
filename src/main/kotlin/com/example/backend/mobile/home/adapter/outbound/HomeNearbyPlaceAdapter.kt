package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.mobile.home.application.port.outbound.HomeNearbyPlacePort
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 내 저장 장소(user, [SavedPlaceUseCase])를 거리순 조회(place, [PlaceQueryUseCase])에 넘기고
 * 방문 여부를 합쳐 BFF 격리 DTO 로 매핑한다.
 * 검색엔진 장애(`PLACE_SEARCH_UNAVAILABLE`)는 경고 로그만 남기고 빈 목록으로 흡수한다(홈 섹션 fail-soft).
 */
@Component
class HomeNearbyPlaceAdapter(
    private val savedPlaceUseCase: SavedPlaceUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
) : HomeNearbyPlacePort {
    private val log = KotlinLogging.logger {}

    override fun findNearbySavedPlaces(
        userId: Long,
        origin: Coordinate,
        limit: Int,
    ): List<HomeNearbyPlace> {
        val refs = savedPlaceUseCase.listSavedPlaceRefs(userId)
        if (refs.isEmpty()) return emptyList()
        val visitedByPlace = refs.associate { it.placeId to it.visited }

        val nearest =
            try {
                placeQueryUseCase.findNearest(refs.map { it.placeId }, origin, limit)
            } catch (e: BusinessException) {
                if (e.errorCode != PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE) throw e
                log.warn { "홈 근처 저장 장소 조회 실패 — 빈 목록으로 대체: userId=$userId" }
                return emptyList()
            }
        return nearest.map {
            HomeNearbyPlace(
                placeId = it.id,
                name = it.name,
                imageUrl = it.imageUrl,
                category = it.category,
                rating = it.rating,
                ratingCount = it.ratingCount,
                distanceMeters = it.distanceMeters,
                visited = visitedByPlace[it.id] ?: false,
            )
        }
    }
}
