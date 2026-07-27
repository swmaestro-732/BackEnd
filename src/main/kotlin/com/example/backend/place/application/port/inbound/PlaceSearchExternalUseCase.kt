package com.example.backend.place.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.ExternalPlace

/** 외부 지도 장소 검색 인바운드 포트(공개 API). 네이버+카카오 결과를 병합·중복제거해 돌려준다. */
interface PlaceSearchExternalUseCase {
    fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace>
}
