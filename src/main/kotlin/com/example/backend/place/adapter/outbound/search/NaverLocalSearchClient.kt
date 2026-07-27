package com.example.backend.place.adapter.outbound.search

import com.example.backend.bootstrap.config.NaverSearchProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.NaverPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 네이버 지역 검색 API 어댑터.
 *
 * `GET /v1/search/local.json?query=&display=5`, 헤더로 클라이언트 ID/시크릿을 붙인다.
 * 응답 `items[]` 를 [ExternalPlace] 로 변환하며(제목 HTML 태그 제거, 좌표는 10^7 로 나눔), 원시 DTO 는 밖으로 내보내지 않는다.
 * 키가 비어 있거나 호출이 실패하면 빈 목록을 돌려준다(fail-soft).
 */
@Component
class NaverLocalSearchClient(
    @param:Qualifier("naverRestClient")
    private val naverRestClient: RestClient,
    private val naverSearchProperties: NaverSearchProperties,
) : NaverPlaceSearchPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> {
        if (naverSearchProperties.clientId.isBlank()) {
            log.warn("네이버 검색 clientId 가 비어 있어 검색을 건너뜁니다.")
            return emptyList()
        }
        return try {
            val response =
                naverRestClient
                    .get()
                    .uri { builder ->
                        builder
                            .path("/v1/search/local.json")
                            .queryParam("query", query)
                            .queryParam("display", DISPLAY)
                            .build()
                    }.header("X-Naver-Client-Id", naverSearchProperties.clientId)
                    .header("X-Naver-Client-Secret", naverSearchProperties.clientSecret)
                    .retrieve()
                    .body(NaverLocalResponse::class.java)
            response?.items.orEmpty().mapNotNull { it.toExternalPlace() }
        } catch (exception: Exception) {
            log.warn("네이버 지역 검색 호출 실패: query={}", query, exception)
            emptyList()
        }
    }

    private fun NaverLocalItem.toExternalPlace(): ExternalPlace? {
        val longitude = mapx?.toDoubleOrNull()?.div(COORDINATE_SCALE) ?: return null
        val latitude = mapy?.toDoubleOrNull()?.div(COORDINATE_SCALE) ?: return null
        return ExternalPlace(
            name = stripHtml(title.orEmpty()),
            category = category.orEmpty(),
            roadAddress = roadAddress?.takeIf(String::isNotBlank),
            address = address?.takeIf(String::isNotBlank),
            coordinate = Coordinate(latitude = latitude, longitude = longitude),
            telephone = telephone?.takeIf(String::isNotBlank),
            source = ExternalPlaceSource.NAVER,
        )
    }

    private fun stripHtml(value: String): String = HTML_TAG.replace(value, "").trim()

    /** 어댑터 내부 전용 원시 응답 DTO — 밖으로 노출하지 않는다. */
    private data class NaverLocalResponse(
        val items: List<NaverLocalItem>? = null,
    )

    private data class NaverLocalItem(
        val title: String? = null,
        val category: String? = null,
        val address: String? = null,
        @param:JsonProperty("roadAddress")
        val roadAddress: String? = null,
        val telephone: String? = null,
        val mapx: String? = null,
        val mapy: String? = null,
    )

    private companion object {
        const val DISPLAY = 5

        // 네이버 지역 검색 좌표는 WGS84 × 10^7 정수 문자열.
        const val COORDINATE_SCALE = 1e7
        val HTML_TAG = Regex("<[^>]*>")
    }
}
