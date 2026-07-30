package com.example.backend.place.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.ExternalPlace

/** 외부 지도 장소 검색 인바운드 포트(공개 API). 카카오 로컬 검색 결과를 돌려준다. */
interface PlaceSearchExternalUseCase {
    fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace>
}
