package com.example.backend.google

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Google Places API(New) 텍스트 검색 호출. 좌표와 `regularOpeningHours` 를 함께 받는다.
 * 실패는 예외로 그대로 던진다. 매칭 판단과 영업시간 변환은 호출하는 어댑터가 한다.
 */
@Component
class GooglePlacesClient(
    @param:Qualifier("googlePlacesRestClient")
    private val googlePlacesRestClient: RestClient,
    private val properties: GooglePlacesProperties,
) {
    /** API 키가 설정됐는지. 비어 있으면 호출하지 않는다. */
    val isConfigured: Boolean
        get() = properties.apiKey.isNotBlank()

    /** `POST /v1/places:searchText`. 중심 좌표 주변으로 치우쳐 검색하고 첫 결과만 돌려준다. */
    fun searchText(
        textQuery: String,
        latitude: Double,
        longitude: Double,
    ): GooglePlace? =
        googlePlacesRestClient
            .post()
            .uri("/v1/places:searchText")
            .header(API_KEY_HEADER, properties.apiKey)
            .header(FIELD_MASK_HEADER, FIELD_MASK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "textQuery" to textQuery,
                    "languageCode" to "ko",
                    "regionCode" to "KR",
                    "pageSize" to 1,
                    "locationBias" to
                        mapOf(
                            "circle" to
                                mapOf(
                                    "center" to
                                        mapOf("latitude" to latitude, "longitude" to longitude),
                                    "radius" to SEARCH_BIAS_RADIUS_METERS,
                                ),
                        ),
                ),
            ).retrieve()
            .body(GoogleTextSearchResponse::class.java)
            ?.places
            ?.firstOrNull()

    private companion object {
        const val API_KEY_HEADER = "X-Goog-Api-Key"
        const val FIELD_MASK_HEADER = "X-Goog-FieldMask"

        // 좌표(매칭 검증)와 영업시간을 검색 응답에서 바로 받는다. 상세 조회를 따로 하지 않는다.
        const val FIELD_MASK = "places.id,places.location,places.regularOpeningHours"
        const val SEARCH_BIAS_RADIUS_METERS = 500.0
    }
}
