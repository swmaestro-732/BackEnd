package com.example.backend.client.tmap

import com.example.backend.common.geo.Coordinate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * T map 보행자 경로 API 클라이언트.
 *
 * `POST /tmap/routes/pedestrian?version=1`, 헤더 `appKey`, JSON 바디 `{startX, startY, endX, endY}` (X=경도, Y=위도).
 * 응답 `features[0].properties.totalTime`(초) → [TmapPedestrianResult.Found].
 *
 * **결과 구분(fail-soft)**:
 * - **[TmapPedestrianResult.NoServiceArea]**: HTTP 4xx + 에러코드 [NO_SERVICE_AREA_CODE](3102) → 경로를 낼 수 없는 구간.
 * - **[TmapPedestrianResult.Failed]**: appKey 미설정 / 그 외 4xx·5xx·네트워크 오류 / 빈 응답.
 *   일시적 오류(Tmap 다운·타임아웃)를 NoServiceArea 로 오인하지 않도록 구분한다.
 */
@Component
class TmapPedestrianClient(
    @param:Qualifier("tmapRestClient")
    private val tmapRestClient: RestClient,
    private val tmapProperties: TmapProperties,
) {
    private val log = KotlinLogging.logger {}

    init {
        if (tmapProperties.appKey.isBlank()) {
            log.warn { "T map appKey 미설정 — 보행자 경로 조회 비활성(fail-soft). 키 주입 후 재기동 필요." }
        }
    }

    fun route(
        from: Coordinate,
        to: Coordinate,
    ): TmapPedestrianResult {
        if (tmapProperties.appKey.isBlank()) return TmapPedestrianResult.Failed // 부팅 시 1회 경고 완료 — 요청별 로깅 생략
        return try {
            val seconds =
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
                    ?.firstOrNull()
                    ?.properties
                    ?.totalTime
            // 200 이지만 경로/시간이 비면 판단 불가 → Failed.
            seconds?.let { TmapPedestrianResult.Found(it) } ?: TmapPedestrianResult.Failed
        } catch (exception: RestClientResponseException) {
            // 4xx/5xx. NoServiceArea(3102)만 경로 없음으로 확정하고, 나머지 오류는 Failed(일시적 오류 가능).
            val code =
                runCatching {
                    exception
                        .getResponseBodyAs(
                            TmapErrorResponse::class.java,
                        )?.error
                        ?.code
                }.getOrNull()
            if (code == NO_SERVICE_AREA_CODE) {
                TmapPedestrianResult.NoServiceArea
            } else {
                log.warn { "T map 보행자 경로 조회 실패(status=${exception.statusCode}, code=$code)" }
                TmapPedestrianResult.Failed
            }
        } catch (exception: Exception) {
            // 네트워크·타임아웃·역직렬화 등 — 일시적 오류일 수 있어 NoServiceArea 와 구분(Failed).
            log.warn(exception) { "T map 보행자 경로 조회 실패" }
            TmapPedestrianResult.Failed
        }
    }

    private companion object {
        /** Tmap NoServiceArea — 도보 경로를 낼 수 없는 구간(예: 물 위·경로 없음). */
        const val NO_SERVICE_AREA_CODE = "3102"
    }
}

sealed interface TmapPedestrianResult {
    data class Found(
        val totalSeconds: Int,
    ) : TmapPedestrianResult

    data object NoServiceArea : TmapPedestrianResult

    data object Failed : TmapPedestrianResult
}
