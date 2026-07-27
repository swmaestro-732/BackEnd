package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.ExternalPlace

/** 네이버 지역 검색 아웃바운드 포트. 키가 없거나 실패하면 빈 목록. */
interface NaverPlaceSearchPort {
    fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace>
}
