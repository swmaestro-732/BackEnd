package com.example.backend.place.adapter.outbound.search

/** OpenSearch 색인 문서 — place 인덱스 매핑(opensearch/place.json)과 필드가 일치한다. */
data class PlaceDocument(
    val name: String,
    val description: String?,
    val category: String,
    val address: String,
    val areaCode: String?,
    val location: GeoLocation,
    val status: String,
)

/** geo_point 직렬화용 — OpenSearch 는 {lat, lon} 형태를 geo_point 로 받는다. */
data class GeoLocation(
    val lat: Double,
    val lon: Double,
)
