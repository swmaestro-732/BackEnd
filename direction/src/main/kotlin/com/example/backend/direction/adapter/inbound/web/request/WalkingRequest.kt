package com.example.backend.direction.adapter.inbound.web.request

import com.example.backend.common.geo.Coordinate
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 도보 시간 요청 — 방문 순서대로 나열한 좌표 목록. 구간 계산을 위해 최소 2개 필요.
 *
 * 상한을 10으로 둔다: 구간(=points-1)마다 Tmap 을 순차 호출하므로 값이 크면 요청 스레드를 오래 점유하고
 * 외부 쿼터를 증폭시킨다. 실제 코스 장소 수를 고려한 현실적 상한이다.
 */
data class WalkingRequest(
    @field:Size(min = 2, max = 10, message = "좌표는 2~10개여야 합니다.")
    @field:Valid
    val points: List<PointDto>,
) {
    fun toCoordinates(): List<Coordinate> = points.map { Coordinate(latitude = it.lat, longitude = it.lng) }
}

data class PointDto(
    @field:NotNull
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val lat: Double,
    @field:NotNull
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val lng: Double,
)
