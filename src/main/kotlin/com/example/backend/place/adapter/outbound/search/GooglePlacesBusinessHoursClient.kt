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
 * Google Places API(New) 영업시간 어댑터. 텍스트 검색 한 번으로 좌표와 `regularOpeningHours` 를 함께 받는다.
 * 질의에 주소를 붙여 같은 이름의 다른 지점(체인점)이 잡히지 않게 하고,
 * 카카오 좌표에서 [MAX_MATCH_DISTANCE_METERS] 보다 먼 결과는 동명이소로 보고 버린다.
 * null 은 호출 실패(다음 동기화에서 재시도), 빈 목록은 매칭·정보 없음(동기화 완료)이다.
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
            val place = searchPlace(name, address, coordinate) ?: return emptyList()
            val location = place.location?.toCoordinate() ?: return emptyList()
            if (distanceMeters(coordinate, location) > MAX_MATCH_DISTANCE_METERS) {
                log.info { "Google Places 매칭 거리 초과로 무시: name=$name, googlePlaceId=${place.id}" }
                return emptyList()
            }
            toBusinessHours(place.regularOpeningHours?.periods.orEmpty())
        } catch (exception: Exception) {
            log.warn(exception) { "Google Places 영업시간 조회 실패: name=$name" }
            null
        }
    }

    private fun searchPlace(
        name: String,
        address: String,
        coordinate: Coordinate,
    ): GooglePlace? =
        googlePlacesRestClient
            .post()
            .uri("/v1/places:searchText")
            .header(API_KEY_HEADER, properties.apiKey)
            .header(FIELD_MASK_HEADER, FIELD_MASK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "textQuery" to "$name $address",
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

    private fun GoogleLatLng.toCoordinate(): Coordinate? {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return Coordinate(latitude = lat, longitude = lng)
    }

    /**
     * Google period → 요일별 한 행. 같은 요일에 period 2개면 브레이크타임으로 보고 그 사이 공백을 쉬는 시간으로 담는다.
     * 연중무휴 24시간(close 없는 period 하나)은 매일 00:00~23:59, 특정 요일만 24시간(00:00 개점·다음 날 00:00 폐점)은
     * 그 요일을 00:00~23:59 로 담고, period 에 없는 요일은 휴무(null) 행으로 채운다.
     * 필드가 빠진 period 는 깨진 응답이므로 휴무로 저장하지 않고 예외로 끝내 다음 동기화에서 재시도되게 한다.
     */
    private fun toBusinessHours(periods: List<GooglePeriod>): List<PlaceBusinessHour> {
        if (periods.isEmpty()) return emptyList()
        if (periods.size == 1 && periods.single().close == null) {
            return (1..7).map { PlaceBusinessHour(it, MIDNIGHT, END_OF_DAY) }
        }
        val byDay =
            periods
                .map { period ->
                    val open = checkNotNull(period.open) { "open 없는 period" }.toLocalTime()
                    val close = checkNotNull(period.close) { "close 없는 period" }.toLocalTime()
                    val day = checkNotNull(period.open.day) { "day 없는 period" }
                    // 00:00 개점 → 다음 날 00:00 폐점은 그날 하루 종일 영업이다. 00:00~00:00 으로 두면 0분 영업과 구별이 안 된다.
                    val effectiveClose = if (open == MIDNIGHT && close == MIDNIGHT) END_OF_DAY else close
                    toIsoDay(day) to (open to effectiveClose)
                }.groupBy({ it.first }, { it.second })
        return (1..7).map { day -> byDay[day].orEmpty().toBusinessHour(day) }
    }

    /**
     * 한 요일의 영업 구간들을 행 하나로 만든다. 영업은 첫 개점~마지막 폐점이고,
     * 구간이 정확히 둘이면 그 사이가 쉬는 시간이다(표본상 셋 이상은 없어 그 경우 쉬는 시간은 담지 않는다).
     */
    private fun List<Pair<LocalTime, LocalTime>>.toBusinessHour(day: Int): PlaceBusinessHour {
        if (isEmpty()) return PlaceBusinessHour(day, null, null)
        val sorted = sortedBy { it.first }
        val breakSpan = if (sorted.size == 2) sorted[0].second to sorted[1].first else null
        return PlaceBusinessHour(
            dayOfWeek = day,
            openTime = sorted.first().first,
            closeTime = sorted.last().second,
            breakStart = breakSpan?.first,
            breakEnd = breakSpan?.second,
        )
    }

    /** 시각 범위 검증은 [LocalTime] 생성자가, 요일 범위 검증은 [PlaceBusinessHour] 가 한다. */
    private fun GoogleTimePoint.toLocalTime(): LocalTime =
        LocalTime(
            checkNotNull(hour) { "hour 없는 period" },
            minute ?: 0,
        )

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

        // 좌표(매칭 검증)와 영업시간을 검색 응답에서 바로 받는다 — 상세 조회를 따로 하지 않는다.
        const val FIELD_MASK = "places.id,places.location,places.regularOpeningHours"
        const val SEARCH_BIAS_RADIUS_METERS = 500.0
        const val MAX_MATCH_DISTANCE_METERS = 300.0
        const val EARTH_RADIUS_METERS = 6_371_000.0
        val MIDNIGHT = LocalTime(0, 0)
        val END_OF_DAY = LocalTime(23, 59)
    }
}
