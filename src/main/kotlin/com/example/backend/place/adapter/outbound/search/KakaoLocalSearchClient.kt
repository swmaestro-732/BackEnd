package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.KakaoLocalProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.KakaoPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 카카오 로컬 키워드 검색 API 어댑터.
 *
 * `GET /v2/local/search/keyword.json?query=&size=15` (near 가 있으면 x/y/radius 추가), 헤더 `Authorization: KakaoAK {restKey}`.
 * 응답 `documents[]` 를 [ExternalPlace] 로 변환한다(원시 DTO 는 밖으로 내보내지 않음).
 * 키가 비어 있거나 호출이 실패하면 빈 목록(fail-soft).
 */
@Component
class KakaoLocalSearchClient(
    @param:Qualifier("kakaoRestClient")
    private val kakaoRestClient: RestClient,
    private val kakaoLocalProperties: KakaoLocalProperties,
) : KakaoPlaceSearchPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> {
        if (kakaoLocalProperties.restKey.isBlank()) {
            log.warn("카카오 로컬 restKey 가 비어 있어 검색을 건너뜁니다.")
            return emptyList()
        }
        return try {
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
            response?.documents.orEmpty().mapNotNull { it.toExternalPlace() }
        } catch (exception: Exception) {
            log.warn("카카오 로컬 검색 호출 실패: query={}", query, exception)
            emptyList()
        }
    }

    private fun KakaoLocalDocument.toExternalPlace(): ExternalPlace? {
        val longitude = x?.toDoubleOrNull() ?: return null
        val latitude = y?.toDoubleOrNull() ?: return null
        return ExternalPlace(
            name = placeName.orEmpty(),
            category = categoryName.orEmpty(),
            roadAddress = roadAddressName?.takeIf(String::isNotBlank),
            address = addressName?.takeIf(String::isNotBlank),
            coordinate = Coordinate(latitude = latitude, longitude = longitude),
            telephone = phone?.takeIf(String::isNotBlank),
            source = ExternalPlaceSource.KAKAO,
        )
    }

    /** 어댑터 내부 전용 원시 응답 DTO — 밖으로 노출하지 않는다. */
    private data class KakaoLocalResponse(
        val documents: List<KakaoLocalDocument>? = null,
    )

    private data class KakaoLocalDocument(
        @param:JsonProperty("place_name")
        val placeName: String? = null,
        @param:JsonProperty("category_name")
        val categoryName: String? = null,
        @param:JsonProperty("road_address_name")
        val roadAddressName: String? = null,
        @param:JsonProperty("address_name")
        val addressName: String? = null,
        val x: String? = null,
        val y: String? = null,
        val phone: String? = null,
    )

    private companion object {
        const val SIZE = 15
        const val RADIUS_METERS = 20000
    }
}
