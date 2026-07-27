package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import com.example.backend.place.application.port.outbound.KakaoPlaceSearchPort
import com.example.backend.place.application.port.outbound.NaverPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import org.springframework.stereotype.Service
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 외부 장소 검색 서비스 — 네이버 결과를 앞세우고 카카오 결과로 채우되 중복을 제거한다.
 *
 * 중복 판정: (정규화 이름[소문자·공백제거]이 같고 좌표가 약 50m 이내) **또는** 도로명 주소가 같으면 같은 장소로 본다.
 */
@Service
class PlaceSearchService(
    private val naverPlaceSearchPort: NaverPlaceSearchPort,
    private val kakaoPlaceSearchPort: KakaoPlaceSearchPort,
) : PlaceSearchExternalUseCase {
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> {
        val naverResults = naverPlaceSearchPort.search(query, near)
        val kakaoResults = kakaoPlaceSearchPort.search(query, near)

        val merged = naverResults.toMutableList()
        for (candidate in kakaoResults) {
            if (merged.none { it.isSamePlace(candidate) }) {
                merged.add(candidate)
            }
        }
        return merged
    }

    private fun ExternalPlace.isSamePlace(other: ExternalPlace): Boolean {
        val sameRoadAddress =
            roadAddress != null &&
                other.roadAddress != null &&
                roadAddress.trim() == other.roadAddress.trim()
        if (sameRoadAddress) return true

        return name.normalized() == other.name.normalized() &&
            distanceMeters(coordinate, other.coordinate) <= DEDUPE_DISTANCE_METERS
    }

    private fun String.normalized(): String = lowercase().filterNot(Char::isWhitespace)

    /** 하버사인 거리(m). ~50m 이내면 같은 지점으로 취급. */
    private fun distanceMeters(
        a: Coordinate,
        b: Coordinate,
    ): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h))
    }

    private companion object {
        const val DEDUPE_DISTANCE_METERS = 50.0
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
