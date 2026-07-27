package com.example.backend.direction.adapter.inbound.web.request

import com.example.backend.common.geo.Coordinate
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 도보 시간 요청 — 방문 순서대로 나열한 좌표 목록. 구간 계산을 위해 최소 2개 필요.
 */
data class WalkingRequest(
    @field:Size(min = 2, message = "좌표는 최소 2개가 필요합니다.")
    @field:Valid
    val points: List<PointDto>,
) {
    fun toCoordinates(): List<Coordinate> = points.map { Coordinate(latitude = it.lat, longitude = it.lng) }
}

data class PointDto(
    @field:NotNull
    val lat: Double,
    @field:NotNull
    val lng: Double,
)
