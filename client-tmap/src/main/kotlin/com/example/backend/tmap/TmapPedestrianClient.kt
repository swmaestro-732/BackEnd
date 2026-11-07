package com.example.backend.tmap

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

/**
 * T map 보행자 경로 API 호출. `POST /tmap/routes/pedestrian?version=1`, 헤더 `appKey`, JSON 바디.
 * 4xx/5xx 는 [RestClientResponseException], 네트워크 오류는 그 밖의 예외로 그대로 던진다.
 * 결과 구분(도보 불가, 모름)과 도메인 변환은 호출하는 어댑터가 한다.
 */
@Component
class TmapPedestrianClient(
    @param:Qualifier("tmapRestClient")
    private val tmapRestClient: RestClient,
    private val tmapProperties: TmapProperties,
) {
    /** appKey 가 설정됐는지. 비어 있으면 호출하지 않는다. */
    val isConfigured: Boolean
        get() = tmapProperties.appKey.isNotBlank()

    fun pedestrianRoute(request: PedestrianRequest): PedestrianResponse? =
        tmapRestClient
            .post()
            .uri { builder -> builder.path("/tmap/routes/pedestrian").queryParam("version", 1).build() }
            .header("appKey", tmapProperties.appKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(PedestrianResponse::class.java)

    /** 4xx/5xx 응답 본문의 T map 에러 코드. 본문이 없거나 형식이 다르면 null. */
    fun errorCode(exception: RestClientResponseException): String? =
        runCatching {
            exception
                .getResponseBodyAs(
                    TmapErrorResponse::class.java,
                )?.error
                ?.code
        }.getOrNull()
}
