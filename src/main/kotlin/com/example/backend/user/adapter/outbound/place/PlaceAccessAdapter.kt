package com.example.backend.user.adapter.outbound.place

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.outbound.PlaceAccessPort
import com.example.backend.user.application.port.outbound.PlaceRef
import org.springframework.stereotype.Component

/**
 * ACL(anti-corruption layer) 어댑터 — user 의 [PlaceAccessPort] 를 place 도메인의 인바운드 포트로 위임하고,
 * place 의 요약(PlaceSummary)을 user 소유 DTO([PlaceRef])로 변환한다.
 * place 를 아는 유일한 지점이며, MSA 분리 시 이 클래스만 REST 클라이언트로 교체하면 된다.
 */
@Component
class PlaceAccessAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
) : PlaceAccessPort {
    override fun findPlace(placeId: Long): PlaceRef? = findPlaces(listOf(placeId)).firstOrNull()

    override fun findPlaces(placeIds: List<Long>): List<PlaceRef> =
        placeQueryUseCase.findPlacesById(placeIds).map {
            PlaceRef(
                id = it.id,
                name = it.name,
                category = it.category,
                imageUrl = it.imageUrl,
                location = Coordinate(it.latitude, it.longitude),
                rating = it.rating,
                ratingCount = it.ratingCount,
            )
        }
}
