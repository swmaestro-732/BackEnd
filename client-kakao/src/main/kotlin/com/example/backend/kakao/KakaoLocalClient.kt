package com.example.backend.kakao

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 카카오 로컬 API 호출. 헤더 `Authorization: KakaoAK {restKey}`.
 * 실패(키 미설정으로 인한 401 포함)는 예외로 그대로 던진다. fail-soft 처리와 도메인 변환은 호출하는 어댑터가 한다.
 */
@Component
class KakaoLocalClient(
    @param:Qualifier("kakaoRestClient")
    private val kakaoRestClient: RestClient,
    private val kakaoLocalProperties: KakaoLocalProperties,
) {
    /** `GET /v2/local/search/keyword.json?query=&size=15`. 중심 좌표가 있으면 x/y/radius 를 붙인다. */
    fun searchKeyword(
        query: String,
        longitude: Double?,
        latitude: Double?,
    ): KakaoLocalResponse? =
        kakaoRestClient
            .get()
            .uri { builder ->
                builder
                    .path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", SIZE)
                    .apply {
                        if (longitude != null && latitude != null) {
                            queryParam("x", longitude)
                            queryParam("y", latitude)
                            queryParam("radius", RADIUS_METERS)
                        }
                    }.build()
            }.header("Authorization", "KakaoAK ${kakaoLocalProperties.restKey}")
            .retrieve()
            .body(KakaoLocalResponse::class.java)

    /** `GET /v2/local/geo/coord2regioncode.json?x=&y=`. 좌표의 행정구역(법정동, 행정동) 문서를 돌려준다. */
    fun coordToRegionCode(
        longitude: Double,
        latitude: Double,
    ): KakaoRegionCodeResponse? =
        kakaoRestClient
            .get()
            .uri { builder ->
                builder
                    .path("/v2/local/geo/coord2regioncode.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .build()
            }.header("Authorization", "KakaoAK ${kakaoLocalProperties.restKey}")
            .retrieve()
            .body(KakaoRegionCodeResponse::class.java)

    private companion object {
        const val SIZE = 15
        const val RADIUS_METERS = 20000
    }
}
