package com.example.backend.place.application.port.inbound

import com.example.backend.common.geo.Viewport
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult

interface PlaceMapQueryUseCase {
    fun searchMap(
        query: String,
        viewport: Viewport,
        category: String?,
    ): PlaceMapResult
}
