package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import com.example.backend.tmap.PedestrianRequest
import com.example.backend.tmap.PedestrianResponse
import com.example.backend.tmap.TmapPedestrianClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode

/**
 * T map 보행자 경로 어댑터. 호출은 [TmapPedestrianClient] 가 하고 여기서는 결과를 도메인 경로로 옮긴다.
 *
 * 응답 `features[0].properties.totalTime`(초)·`totalDistance`(m) 와 LineString 좌표·Point 안내 문구 → [PedestrianRoute.Reachable].
 *
 * **결과 구분(fail-soft)**:
 * - **[PedestrianRoute.Unreachable]**: Tmap 이 HTTP 4xx + 에러코드 [NO_SERVICE_AREA_CODE](3102, NoServiceArea)로 응답 →
 *   도보로 갈 수 없는 구간. (프론트 "걸어갈 수 없는 거리".)
 * - **[PedestrianRoute.Unknown]**: appKey 미설정 / 그 외 4xx·5xx·네트워크 오류(`retrieve()` 가 예외로 던짐) / 빈 응답 →
 *   "모름". 일시적 오류(Tmap 다운·타임아웃)를 도보 불가로 오인하지 않도록 [Unreachable] 과 구분한다.
 */
@Component
class TmapPedestrianAdapter(
    private val tmapPedestrianClient: TmapPedestrianClient,
) : PedestrianRoutePort {
    private val log = KotlinLogging.logger {}

    init {
        if (!tmapPedestrianClient.isConfigured) {
            log.warn { "T map appKey 미설정 — 보행자 경로 조회 비활성(fail-soft). 키 주입 후 재기동 필요." }
        }
    }

    override fun walkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): PedestrianRoute {
        if (!tmapPedestrianClient.isConfigured) return PedestrianRoute.Unknown // 부팅 시 1회 경고 완료, 요청별 로깅 생략
        return try {
            val features =
                tmapPedestrianClient
                    .pedestrianRoute(
                        PedestrianRequest(
                            startX = from.longitude,
                            startY = from.latitude,
                            endX = to.longitude,
                            endY = to.latitude,
                        ),
                    )?.features
                    .orEmpty()
            // 200 이지만 시간·거리·경로 중 하나라도 비면 판단 불가 → Unknown.
            toReachable(features) ?: PedestrianRoute.Unknown
        } catch (exception: RestClientResponseException) {
            // 4xx/5xx. NoServiceArea(3102)만 도보 불가로 확정하고, 나머지 오류는 Unknown(일시적 오류 가능).
            val code = tmapPedestrianClient.errorCode(exception)
            if (code == NO_SERVICE_AREA_CODE) {
                PedestrianRoute.Unreachable
            } else {
                log.warn { "T map 보행자 경로 조회 실패(status=${exception.statusCode}, code=$code)" }
                PedestrianRoute.Unknown
            }
        } catch (exception: Exception) {
            // 네트워크·타임아웃·역직렬화 등 — 일시적 오류일 수 있어 도보 불가와 구분(Unknown).
            log.warn(exception) { "T map 보행자 경로 조회 실패" }
            PedestrianRoute.Unknown
        }
    }

    private fun toReachable(features: List<PedestrianResponse.Feature>): PedestrianRoute.Reachable? {
        val summary = features.firstOrNull()?.properties ?: return null
        val seconds = summary.totalTime ?: return null
        val distanceMeters = summary.totalDistance ?: return null
        val path = mutableListOf<Coordinate>()
        val guides = mutableListOf<PedestrianRoute.Guide>()
        features.forEach { feature ->
            val geometry = feature.geometry ?: return@forEach
            when (geometry.type) {
                "Point" -> {
                    geometry.coordinates?.toCoordinate()?.let { point ->
                        val description = feature.properties?.description?.takeIf { it.isNotBlank() } ?: return@let
                        guides += PedestrianRoute.Guide(point, description)
                    }
                }

                "LineString" -> {
                    geometry.coordinates?.forEach { node ->
                        val point = node.toCoordinate() ?: return@forEach
                        // 선분 경계는 앞 선분의 끝점과 같은 좌표로 시작하므로 한 번만 담는다.
                        if (path.lastOrNull() != point) path += point
                    }
                }
            }
        }
        // 그릴 선이 없으면 walkable=true 로 내보낼 수 없다 → Unknown(외부 지도 폴백).
        if (path.isEmpty()) return null
        return PedestrianRoute.Reachable(
            seconds = seconds,
            distanceMeters = distanceMeters,
            path = path,
            guides = guides,
        )
    }

    /** GeoJSON `[x, y]` → [Coordinate] (x=경도, y=위도). 형식이 어긋나면 null. */
    private fun JsonNode.toCoordinate(): Coordinate? {
        if (!isArray || size() < 2) return null
        val longitude = get(0).takeIf { it.isNumber }?.asDouble() ?: return null
        val latitude = get(1).takeIf { it.isNumber }?.asDouble() ?: return null
        return runCatching { Coordinate(latitude = latitude, longitude = longitude) }.getOrNull()
    }

    private companion object {
        /** Tmap NoServiceArea — 도보 경로를 낼 수 없는 구간(예: 물 위·경로 없음). */
        const val NO_SERVICE_AREA_CODE = "3102"
    }
}
