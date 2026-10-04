package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceSearchResult

/**
 * 외부 지도 장소 검색 응답 — 검색 후 내부 저장(dedup)까지 마친 결과를 웹 표현으로 매핑한다.
 * 각 항목은 우리 내부 place id 를 함께 실어 나른다.
 */
data class ExternalPlaceSearchResponse(
    val places: List<Item>,
) {
    data class Item(
        val id: Long,
        val name: String,
        val category: String,
        val roadAddress: String?,
        val address: String?,
        val latitude: Double,
        val longitude: Double,
        val telephone: String?,
    )

    companion object {
        fun from(places: List<PlaceSearchResult>): ExternalPlaceSearchResponse =
            ExternalPlaceSearchResponse(
                places =
                    places.map {
                        Item(
                            id = it.id,
                            name = it.name,
                            category = it.category,
                            roadAddress = it.roadAddress,
                            address = it.address,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            telephone = it.telephone,
                        )
                    },
            )
    }
}
