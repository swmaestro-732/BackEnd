package com.example.backend.place.adapter.outbound.search

/** Google Places API(New) 텍스트 검색 원시 응답 — 어댑터 밖으로 노출하지 않는다. */
internal data class GoogleTextSearchResponse(
    val places: List<GooglePlace>? = null,
)

internal data class GooglePlace(
    val id: String? = null,
    val location: GoogleLatLng? = null,
    val regularOpeningHours: GoogleOpeningHours? = null,
)

internal data class GoogleLatLng(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

internal data class GoogleOpeningHours(
    val periods: List<GooglePeriod>? = null,
)

/** close 가 없으면(open 만 day=0 00:00) 24시간 영업. */
internal data class GooglePeriod(
    val open: GoogleTimePoint? = null,
    val close: GoogleTimePoint? = null,
)

/** day 는 0=일 … 6=토. */
internal data class GoogleTimePoint(
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
)
