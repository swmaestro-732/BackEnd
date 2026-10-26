package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceRegistrationUseCase
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSearchResult
import com.example.backend.place.application.port.inbound.dto.RegisterPlaceCommand
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import org.springframework.stereotype.Service

/** 카카오 검색 결과를 공통 등록 경로로 저장하고 검색 응답의 표시 필드를 유지한다. */
@Service
class PlaceSearchService(
    private val externalPlaceSearchPort: ExternalPlaceSearchPort,
    private val placeRegistrationUseCase: PlaceRegistrationUseCase,
) : PlaceSearchExternalUseCase {
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<PlaceSearchResult> {
        val external = externalPlaceSearchPort.search(query, near)
        if (external.isEmpty()) return emptyList()

        val placeByKakao =
            placeRegistrationUseCase
                .register(
                    external.map { ext ->
                        RegisterPlaceCommand(
                            kakaoPlaceId = ext.kakaoPlaceId,
                            name = ext.name,
                            category = ext.category.name,
                            coordinate = ext.coordinate,
                            address = (ext.roadAddress ?: ext.address).orEmpty(),
                        )
                    },
                ).associateBy { it.kakaoPlaceId }

        return external.mapNotNull { ext ->
            val place = placeByKakao[ext.kakaoPlaceId] ?: return@mapNotNull null
            PlaceSearchResult(
                id = place.id,
                name = ext.name,
                category = place.category,
                roadAddress = ext.roadAddress,
                address = ext.address,
                latitude = ext.coordinate.latitude,
                longitude = ext.coordinate.longitude,
                telephone = ext.telephone,
            )
        }
    }
}
