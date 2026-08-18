package com.example.backend.mobile.place.adapter.outbound

import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import com.example.backend.mobile.place.application.port.outbound.dto.ScreenPlace
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 장소 상세 화면 후보 조회를 place 도메인 인바운드 포트에 위임하고 BFF 격리 DTO 로 매핑한다.
 * (MSA 분리 시 이 어댑터만 place 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class ScreenPlaceAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
) : ScreenPlacePort {
    override fun findById(placeId: Long): ScreenPlace? = findByIds(listOf(placeId)).firstOrNull()

    override fun findByIds(placeIds: List<Long>): List<ScreenPlace> =
        placeQueryUseCase.findPlacesById(placeIds).map { it.toScreenPlace() }

    private fun PlaceSummary.toScreenPlace() =
        ScreenPlace(
            id = id,
            name = name,
            category = category,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
            address = address,
            areaCode = areaCode,
        )
}
