package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.ExternalPlace

/** 외부 장소 검색 아웃바운드 포트(제공자 중립). 호출 실패 시 빈 목록. */
interface ExternalPlaceSearchPort {
    fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace>
}
