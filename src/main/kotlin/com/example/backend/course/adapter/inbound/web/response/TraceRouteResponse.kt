package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.inbound.dto.TracePlaceResult
import com.example.backend.course.application.port.inbound.dto.TraceRouteResult
import kotlin.math.ceil

/**
 * 따라가기 경로 응답 — 출발·도착 장소와 도보 경로.
 * - [walkable] false 면 도보로 갈 수 없는 구간(Tmap 서비스 불가): 거리·시간은 null, path·guides 는 빈 배열.
 * - [path] 는 지도에 그릴 폴리라인(순서대로 이어진 좌표), [guides] 는 출발·회전·도착 안내 지점.
 */
data class TraceRouteResponse(
    val from: PlacePointResponse,
    val to: PlacePointResponse,
    val walkable: Boolean,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val durationMinutes: Int?,
    val path: List<PointResponse>,
    val guides: List<GuideResponse>,
) {
    data class PlacePointResponse(
        val placeId: Long,
        val name: String,
        val latitude: Double,
        val longitude: Double,
    )

    data class PointResponse(
        val latitude: Double,
        val longitude: Double,
    )

    data class GuideResponse(
        val latitude: Double,
        val longitude: Double,
        val description: String,
    )

    companion object {
        fun from(result: TraceRouteResult): TraceRouteResponse {
            val route = result.route
            return TraceRouteResponse(
                from = result.from.toResponse(),
                to = result.to.toResponse(),
                walkable = route != null,
                distanceMeters = route?.distanceMeters,
                durationSeconds = route?.seconds,
                durationMinutes = route?.let { ceil(it.seconds / 60.0).toInt() },
                path = route?.path.orEmpty().map { it.toResponse() },
                guides =
                    route?.guides.orEmpty().map {
                        GuideResponse(it.coordinate.latitude, it.coordinate.longitude, it.description)
                    },
            )
        }

        private fun TracePlaceResult.toResponse() =
            PlacePointResponse(placeId, name, coordinate.latitude, coordinate.longitude)

        private fun Coordinate.toResponse() = PointResponse(latitude, longitude)
    }
}
