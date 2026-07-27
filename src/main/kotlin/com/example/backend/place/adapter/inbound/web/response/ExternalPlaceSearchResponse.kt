package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.domain.model.ExternalPlace

/**
 * 외부 지도 장소 검색 응답 — 네이버+카카오 병합 결과를 웹 표현으로 매핑한다.
 * 도메인 모델([ExternalPlace])을 직접 노출하지 않고 평면 DTO 로 변환한다.
 */
data class ExternalPlaceSearchResponse(
    val places: List<Item>,
) {
    data class Item(
        val name: String,
        val category: String,
        val roadAddress: String?,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val telephone: String?,
        val source: String,
    )

    companion object {
        fun from(places: List<ExternalPlace>): ExternalPlaceSearchResponse =
            ExternalPlaceSearchResponse(
                places =
                    places.map {
                        Item(
                            name = it.name,
                            category = it.category,
                            roadAddress = it.roadAddress,
                            address = it.address,
                            latitude = it.coordinate.latitude,
                            longitude = it.coordinate.longitude,
                            telephone = it.telephone,
                            source = it.source.name,
                        )
                    },
            )
    }
}
