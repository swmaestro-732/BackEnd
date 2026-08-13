package com.example.backend.course.adapter.outbound.place

import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlaceRef
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.springframework.stereotype.Component

/**
 * ACL(anti-corruption layer) 어댑터 — course 의 [PlaceLookupPort] 를 place 도메인의 인바운드 포트로 위임하고,
 * place 의 요약(PlaceSummary)을 course 소유 DTO([PlaceRef])로 변환한다.
 * place 를 아는 유일한 지점이며, MSA 분리 시 이 클래스만 REST 클라이언트로 교체하면 된다.
 */
@Component
class PlaceLookupAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
) : PlaceLookupPort {
    override fun findPlacesByIds(placeIds: List<Long>): List<PlaceRef> =
        placeQueryUseCase
            .findPlacesById(placeIds)
            .map { PlaceRef(id = it.id, category = it.category, areaCode = it.areaCode) }
}
