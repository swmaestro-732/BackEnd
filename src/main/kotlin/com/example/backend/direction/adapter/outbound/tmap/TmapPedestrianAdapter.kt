package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.bootstrap.config.TmapProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * T map 보행자 경로 API 어댑터.
 *
 * `POST /tmap/routes/pedestrian?version=1`, 헤더 `appKey`, JSON 바디 `{startX, startY, endX, endY, startName, endName}`
 * (X=경도, Y=위도). 응답 `features[0].properties.totalTime`(초)를 반환한다.
 * appKey 가 비었거나 실패/빈 응답이면 null(fail-soft).
 */
@Component
class TmapPedestrianAdapter(
    @param:Qualifier("tmapRestClient")
    private val tmapRestClient: RestClient,
    private val tmapProperties: TmapProperties,
) : PedestrianRoutePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun walkingSeconds(
        from: Coordinate,
        to: Coordinate,
    ): Int? {
        if (tmapProperties.appKey.isBlank()) {
            log.warn("T map appKey 가 비어 있어 보행자 경로 조회를 건너뜁니다.")
            return null
        }
        return try {
            val response =
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
            response
                ?.features
                ?.firstOrNull()
                ?.properties
                ?.totalTime
        } catch (exception: Exception) {
            log.warn("T map 보행자 경로 조회 실패", exception)
            null
        }
    }

    /** 어댑터 내부 전용 요청 DTO. */
    private data class PedestrianRequest(
        val startX: Double,
        val startY: Double,
        val endX: Double,
        val endY: Double,
        val startName: String = "출발",
        val endName: String = "도착",
    )

    /** 어댑터 내부 전용 응답 DTO — 밖으로 노출하지 않는다. */
    private data class PedestrianResponse(
        val features: List<Feature>? = null,
    )

    private data class Feature(
        val properties: Properties? = null,
    )

    private data class Properties(
        val totalTime: Int? = null,
    )
}
