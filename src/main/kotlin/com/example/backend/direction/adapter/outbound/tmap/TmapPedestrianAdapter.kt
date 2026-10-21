package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.bootstrap.config.TmapProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode

/**
 * T map 보행자 경로 API 어댑터.
 *
 * `POST /tmap/routes/pedestrian?version=1`, 헤더 `appKey`, JSON 바디 `{startX, startY, endX, endY}` (X=경도, Y=위도).
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
    @param:Qualifier("tmapRestClient")
    private val tmapRestClient: RestClient,
    private val tmapProperties: TmapProperties,
) : PedestrianRoutePort {
    private val log = KotlinLogging.logger {}

    init {
        if (tmapProperties.appKey.isBlank()) {
            log.warn { "T map appKey 미설정 — 보행자 경로 조회 비활성(fail-soft). 키 주입 후 재기동 필요." }
        }
    }

    override fun walkingRoute(
        from: Coordinate,
        to: Coordinate,
    ): PedestrianRoute {
        if (tmapProperties.appKey.isBlank()) return PedestrianRoute.Unknown // 부팅 시 1회 경고 완료 — 요청별 로깅 생략
        return try {
            val features =
                tmapRestClient
                    .post()
                    .uri { builder -> builder.path("/tmap/routes/pedestrian").queryParam("version", 1).build() }
                    .header("appKey", tmapProperties.appKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        PedestrianRequest(
                            startX = from.longitude,
                            startY = from.latitude,
                            endX = to.longitude,
                            endY = to.latitude,
                        ),
                    ).retrieve()
                    .body(PedestrianResponse::class.java)
                    ?.features
                    .orEmpty()
            // 200 이지만 경로/시간이 비면 판단 불가 → Unknown.
            toReachable(features) ?: PedestrianRoute.Unknown
        } catch (exception: RestClientResponseException) {
            // 4xx/5xx. NoServiceArea(3102)만 도보 불가로 확정하고, 나머지 오류는 Unknown(일시적 오류 가능).
            val code =
                runCatching {
                    exception
                        .getResponseBodyAs(
                            TmapErrorResponse::class.java,
                        )?.error
                        ?.code
                }.getOrNull()
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
        return PedestrianRoute.Reachable(
            seconds = seconds,
            distanceMeters = summary.totalDistance ?: 0,
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
