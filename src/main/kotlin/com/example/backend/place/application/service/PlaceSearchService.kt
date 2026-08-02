package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.persistence.postgis.GeoPoint
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSearchResult
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.application.port.outbound.PlacePersistencePort
import com.example.backend.place.domain.model.Place
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 외부 장소 검색 서비스 — 카카오 로컬 키워드 검색 후 결과를 내부 places 테이블에 kakao place id 로 dedup 저장한다.
 *
 * 매 호출마다 카카오를 새로(fresh) 조회하되, 이미 저장된 장소는 재삽입 없이 기존 row(내부 id)를 재사용한다.
 * 카카오는 near 좌표를 radius 로 네이티브 지원하고 최대 45개까지 조회 가능해 단일 제공자로 충분하다.
 */
@Service
class PlaceSearchService(
    private val externalPlaceSearchPort: ExternalPlaceSearchPort,
    private val placePersistencePort: PlacePersistencePort,
) : PlaceSearchExternalUseCase {
    @Transactional
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<PlaceSearchResult> {
        val external = externalPlaceSearchPort.search(query, near)
        if (external.isEmpty()) return emptyList()

        // 이미 저장된 장소는 kakao id 로 찾아 재사용한다(kakaoPlaceId 는 카카오 유래 row 에선 non-null).
        val existingByKakao =
            placePersistencePort
                .findByKakaoIds(external.map { it.kakaoPlaceId })
                .mapNotNull { place -> place.kakaoPlaceId?.let { it to place } }
                .toMap()

        // 저장되지 않은 것만 신규 도메인으로. 카카오가 페이지 간 중복 id 를 줄 수 있어 distinctBy 로 한 번 걸러낸다.
        // 도메인 불변식(이름·주소 비어 있으면 예외)에 걸리지 않도록 빈 값은 미리 제외한다.
        val toInsert =
            external
                .asSequence()
                .filter { it.kakaoPlaceId !in existingByKakao }
                .distinctBy { it.kakaoPlaceId }
                .filter { it.name.isNotBlank() }
                .mapNotNull { ext ->
                    val address = ext.roadAddress ?: ext.address ?: return@mapNotNull null
                    if (address.isBlank()) return@mapNotNull null
                    Place.create(
                        name = ext.name,
                        description = null,
                        category = ext.category,
                        location = GeoPoint(ext.coordinate.latitude, ext.coordinate.longitude),
                        address = address,
                        imageUrl = null,
                        kakaoPlaceId = ext.kakaoPlaceId,
                    )
                }.toList()

        val inserted =
            placePersistencePort
                .saveAll(toInsert)
                .mapNotNull { place -> place.kakaoPlaceId?.let { it to place } }
                .toMap()

        return external.mapNotNull { ext ->
            val place = existingByKakao[ext.kakaoPlaceId] ?: inserted[ext.kakaoPlaceId] ?: return@mapNotNull null
            PlaceSearchResult(
                id = place.id!!,
                name = ext.name,
                category = place.category.name,
                roadAddress = ext.roadAddress,
                address = ext.address,
                latitude = ext.coordinate.latitude,
                longitude = ext.coordinate.longitude,
                telephone = ext.telephone,
            )
        }
    }
}
