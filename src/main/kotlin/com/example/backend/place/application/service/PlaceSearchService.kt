package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.inbound.PlaceSearchExternalUseCase
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import org.springframework.stereotype.Service

/**
 * 외부 장소 검색 서비스 — 카카오 로컬 키워드 검색에 위임한다.
 *
 * 카카오는 near 좌표를 radius 로 네이티브 지원하고 최대 45개까지 조회 가능해 단일 제공자로 충분하다.
 * (네이버 지역검색은 5개 캡·위치 바이어스 없음이라 제거 — 필요 시 별도 제공자 병합으로 재도입.)
 */
@Service
class PlaceSearchService(
    private val externalPlaceSearchPort: ExternalPlaceSearchPort,
) : PlaceSearchExternalUseCase {
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> = externalPlaceSearchPort.search(query, near)
}
