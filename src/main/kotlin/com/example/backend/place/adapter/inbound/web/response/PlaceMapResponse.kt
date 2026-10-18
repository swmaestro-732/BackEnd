package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceMapResult

/** places와 clusters를 함께 지도에 표시한다. totalCount는 두 배열이 나타내는 장소의 총수다. */
data class PlaceMapResponse(
    val totalCount: Long,
    val places: List<PlaceMapMarkerResponse>,
    val clusters: List<PlaceMapClusterResponse>,
) {
    companion object {
        fun from(result: PlaceMapResult) =
            PlaceMapResponse(
                totalCount = result.totalCount,
                places =
                    result.places.map {
                        PlaceMapMarkerResponse(
                            it.id,
                            it.name,
                            it.category,
                            it.latitude,
                            it.longitude,
                        )
                    },
                clusters =
                    result.clusters.map {
                        PlaceMapClusterResponse(
                            it.key,
                            it.latitude,
                            it.longitude,
                            it.count,
                        )
                    },
            )
    }
}

data class PlaceMapMarkerResponse(
    val id: Long,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
)

data class PlaceMapClusterResponse(
    val key: String,
    val latitude: Double,
    val longitude: Double,
    val count: Long,
)
