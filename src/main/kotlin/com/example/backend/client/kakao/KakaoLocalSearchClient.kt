package com.example.backend.client.kakao

import com.example.backend.common.geo.Coordinate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 카카오 로컬 키워드 검색 API 클라이언트.
 *
 * `GET /v2/local/search/keyword.json?query=&size=15` (near 가 있으면 x/y/radius 추가), 헤더 `Authorization: KakaoAK {restKey}`.
 * 호출이 실패하면(키 미설정으로 인한 401 포함) 빈 목록을 돌려준다(fail-soft).
 */
@Component
class KakaoLocalSearchClient(
    @param:Qualifier("kakaoRestClient")
    private val kakaoRestClient: RestClient,
    private val kakaoLocalProperties: KakaoLocalProperties,
) {
    private val log = KotlinLogging.logger {}

    fun search(
        query: String,
        near: Coordinate?,
    ): List<KakaoLocalDocument> =
        try {
            val response =
                kakaoRestClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", SIZE)
                            .apply {
                                if (near != null) {
                                    queryParam("x", near.longitude)
                                    queryParam("y", near.latitude)
                                    queryParam("radius", RADIUS_METERS)
                                }
                            }.build()
                    }.header("Authorization", "KakaoAK ${kakaoLocalProperties.restKey}")
                    .retrieve()
                    .body(KakaoLocalResponse::class.java)
            response?.documents.orEmpty()
        } catch (exception: Exception) {
            log.warn(exception) { "카카오 로컬 검색 호출 실패: query=$query" }
            emptyList()
        }

    private companion object {
        const val SIZE = 15
        const val RADIUS_METERS = 20000
    }
}
