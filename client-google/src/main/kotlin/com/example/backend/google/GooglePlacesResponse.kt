package com.example.backend.google

/** Google Places API(New) 텍스트 검색 원시 응답. 영업시간 변환은 이 클라이언트를 쓰는 어댑터가 한다. */
data class GoogleTextSearchResponse(
    val places: List<GooglePlace>? = null,
)

data class GooglePlace(
    val id: String? = null,
    val location: GoogleLatLng? = null,
    val regularOpeningHours: GoogleOpeningHours? = null,
)

data class GoogleLatLng(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class GoogleOpeningHours(
    val periods: List<GooglePeriod>? = null,
)

/** close 가 없으면(open 만 day=0 00:00) 24시간 영업. */
data class GooglePeriod(
    val open: GoogleTimePoint? = null,
    val close: GoogleTimePoint? = null,
)

/** day 는 0=일 … 6=토. */
data class GoogleTimePoint(
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
)
