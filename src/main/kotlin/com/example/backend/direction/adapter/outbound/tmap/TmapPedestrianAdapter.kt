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

    init {
        if (tmapProperties.appKey.isBlank()) {
            log.warn("T map appKey 미설정 — 보행자 경로 조회 비활성(fail-soft). 키 주입 후 재기동 필요.")
        }
    }

    override fun walkingSeconds(
        from: Coordinate,
        to: Coordinate,
    ): Int? {
        if (tmapProperties.appKey.isBlank()) return null // 부팅 시 1회 경고 완료 — 요청별 로깅은 생략
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
}
