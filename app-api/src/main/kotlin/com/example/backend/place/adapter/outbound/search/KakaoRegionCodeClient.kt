package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.KakaoLocalProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 카카오 좌표→행정구역(coord2regioncode) API 어댑터.
 *
 * `GET /v2/local/geo/coord2regioncode.json?x=&y=`, 헤더 `Authorization: KakaoAK {restKey}`.
 * 응답 `documents[]` 중 법정동(region_type="B") 문서의 code(법정동코드 10자리)를 돌려준다.
 * 저장 흐름의 부가 정보 조회라 실패하면(키 미설정으로 인한 401 포함) null 을 돌려준다(fail-soft).
 */
@Component
class KakaoRegionCodeClient(
    @param:Qualifier("kakaoRestClient")
    private val kakaoRestClient: RestClient,
    private val kakaoLocalProperties: KakaoLocalProperties,
) : AreaCodeLookupPort {
    private val log = KotlinLogging.logger {}

    override fun findLegalDongCode(coordinate: Coordinate): String? =
        try {
            val response =
                kakaoRestClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/v2/local/geo/coord2regioncode.json")
                            .queryParam("x", coordinate.longitude)
                            .queryParam("y", coordinate.latitude)
                            .build()
                    }.header("Authorization", "KakaoAK ${kakaoLocalProperties.restKey}")
                    .retrieve()
                    .body(KakaoRegionCodeResponse::class.java)
            response
                ?.documents
                .orEmpty()
                .firstOrNull { it.regionType == LEGAL_DONG_TYPE }
                ?.code
                ?.takeIf { it.matches(LEGAL_DONG_CODE_PATTERN) }
        } catch (exception: Exception) {
            log.warn(exception) { "카카오 좌표→법정동 변환 호출 실패: coordinate=$coordinate" }
            null
        }

    private companion object {
        const val LEGAL_DONG_TYPE = "B"
        val LEGAL_DONG_CODE_PATTERN = Regex("^[0-9]{10}$")
    }
}
