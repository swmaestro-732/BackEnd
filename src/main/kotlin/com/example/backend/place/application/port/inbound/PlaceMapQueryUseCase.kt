package com.example.backend.place.application.port.inbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.common.geo.Viewport
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult
import com.example.backend.place.application.port.inbound.dto.PlaceMapSort

interface PlaceMapQueryUseCase {
    /** [userLocation] 은 DISTANCE 정렬 기준점(없으면 뷰포트 중심). 개별 마커 순서에만 적용되고 클러스터는 무관하다. */
    fun searchMap(
        query: String,
        viewport: Viewport,
        category: String?,
        sort: PlaceMapSort = PlaceMapSort.RELEVANCE,
        userLocation: Coordinate? = null,
    ): PlaceMapResult
}
