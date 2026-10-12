package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.GooglePlacesProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.BusinessHoursLookupPort
import com.example.backend.place.domain.model.PlaceBusinessHour
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalTime
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Google Places API(New) 영업시간 어댑터.
 *
 * 1) `POST /v1/places:searchText` — 이름을 질의하고 카카오 좌표로 locationBias, 필드마스크 `places.id`(ID 전용 SKU, 무료).
 * 2) `GET /v1/places/{id}` — 필드마스크 `location,regularOpeningHours`(Enterprise SKU, 과금).
 * 상세 좌표가 카카오 좌표에서 [MAX_MATCH_DISTANCE_METERS] 보다 멀면 다른 장소로 보고 빈 목록을 준다.
 * 키 미설정·호출 실패는 null(재시도 대상), 매칭 없음·영업시간 없음은 빈 목록(동기화 완료)으로 구분한다.
 */
@Component
class GooglePlacesBusinessHoursClient(
    @param:Qualifier("googlePlacesRestClient")
    private val googlePlacesRestClient: RestClient,
    private val properties: GooglePlacesProperties,
) : BusinessHoursLookupPort {
    private val log = KotlinLogging.logger {}

    override fun findBusinessHours(
        name: String,
        address: String,
        coordinate: Coordinate,
    ): List<PlaceBusinessHour>? {
        if (properties.apiKey.isBlank()) return null
        return try {
            val placeId = searchPlaceId(name, coordinate) ?: return emptyList()
            val detail = fetchDetail(placeId) ?: return emptyList()
            val location = detail.location?.toCoordinate() ?: return emptyList()
            if (distanceMeters(coordinate, location) > MAX_MATCH_DISTANCE_METERS) {
                log.info { "Google Places 매칭 거리 초과로 무시: name=$name, googlePlaceId=$placeId" }
                return emptyList()
            }
            toBusinessHours(detail.regularOpeningHours?.periods.orEmpty())
        } catch (exception: Exception) {
            log.warn(exception) { "Google Places 영업시간 조회 실패: name=$name" }
            null
        }
    }

    private fun searchPlaceId(
        name: String,
        coordinate: Coordinate,
    ): String? =
        googlePlacesRestClient
            .post()
            .uri("/v1/places:searchText")
            .header(API_KEY_HEADER, properties.apiKey)
            .header(FIELD_MASK_HEADER, "places.id")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "textQuery" to name,
                    "languageCode" to "ko",
                    "regionCode" to "KR",
                    "pageSize" to 1,
                    "locationBias" to
                        mapOf(
                            "circle" to
                                mapOf(
                                    "center" to
                                        mapOf("latitude" to coordinate.latitude, "longitude" to coordinate.longitude),
                                    "radius" to SEARCH_BIAS_RADIUS_METERS,
                                ),
                        ),
                ),
            ).retrieve()
            .body(GoogleTextSearchResponse::class.java)
            ?.places
            ?.firstOrNull()
            ?.id
            ?.takeIf(String::isNotBlank)

    private fun fetchDetail(placeId: String): GooglePlaceDetail? =
        googlePlacesRestClient
            .get()
            .uri("/v1/places/{id}", placeId)
            .header(API_KEY_HEADER, properties.apiKey)
            .header(FIELD_MASK_HEADER, "location,regularOpeningHours")
            .retrieve()
            .body(GooglePlaceDetail::class.java)

    private fun GoogleLatLng.toCoordinate(): Coordinate? {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return Coordinate(latitude = lat, longitude = lng)
    }

    /**
     * Google period → 요일별 한 행. 같은 요일에 여러 period(브레이크타임)면 첫 개점~마지막 폐점으로 합친다.
     * 24시간 영업(close 없음)은 매일 00:00~23:59, period 에 없는 요일은 휴무(null) 행으로 채운다.
     */
    private fun toBusinessHours(periods: List<GooglePeriod>): List<PlaceBusinessHour> {
        if (periods.isEmpty()) return emptyList()
        if (periods.size == 1 && periods.single().close == null) {
            return (1..7).map { PlaceBusinessHour(it, LocalTime(0, 0), LocalTime(23, 59)) }
        }
        val byDay =
            periods
                .mapNotNull { period ->
                    val open = period.open?.toLocalTime() ?: return@mapNotNull null
                    val close = period.close?.toLocalTime() ?: return@mapNotNull null
                    val day = period.open.day ?: return@mapNotNull null
                    toIsoDay(day) to (open to close)
                }.groupBy({ it.first }, { it.second })
        return (1..7).map { day ->
            val spans = byDay[day]
            if (spans.isNullOrEmpty()) {
                PlaceBusinessHour(day, null, null)
            } else {
                PlaceBusinessHour(day, spans.first().first, spans.last().second)
            }
        }
    }

    private fun GoogleTimePoint.toLocalTime(): LocalTime? {
        val h = hour ?: return null
        if (h !in 0..23) return null
        return LocalTime(h, minute ?: 0)
    }

    private fun toIsoDay(googleDay: Int): Int = if (googleDay == 0) 7 else googleDay

    private fun distanceMeters(
        a: Coordinate,
        b: Coordinate,
    ): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    private companion object {
        const val API_KEY_HEADER = "X-Goog-Api-Key"
        const val FIELD_MASK_HEADER = "X-Goog-FieldMask"
        const val SEARCH_BIAS_RADIUS_METERS = 500.0
        const val MAX_MATCH_DISTANCE_METERS = 300.0
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
