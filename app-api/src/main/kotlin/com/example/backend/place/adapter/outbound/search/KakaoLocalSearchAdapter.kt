package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.kakao.KakaoLocalClient
import com.example.backend.kakao.KakaoLocalDocument
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 카카오 로컬 키워드 검색 어댑터. 호출은 [KakaoLocalClient] 가 하고 여기서는 응답을 도메인으로 옮긴다.
 *
 * 응답 `documents[]` 를 [ExternalPlace] 로 변환한다(원시 DTO 는 밖으로 내보내지 않음).
 * 호출이 실패하면(키 미설정으로 인한 401 포함) 빈 목록을 돌려준다(fail-soft).
 */
@Component
class KakaoLocalSearchAdapter(
    private val kakaoLocalClient: KakaoLocalClient,
) : ExternalPlaceSearchPort {
    private val log = KotlinLogging.logger {}

    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> =
        try {
            val response = kakaoLocalClient.searchKeyword(query, longitude = near?.longitude, latitude = near?.latitude)
            response?.documents.orEmpty().mapNotNull { it.toExternalPlace() }
        } catch (exception: Exception) {
            log.warn(exception) { "카카오 로컬 검색 호출 실패: query=$query" }
            emptyList()
        }

    private fun KakaoLocalDocument.toExternalPlace(): ExternalPlace? {
        val kakaoId = id?.takeIf(String::isNotBlank) ?: return null
        val longitude = x?.toDoubleOrNull() ?: return null
        val latitude = y?.toDoubleOrNull() ?: return null
        return ExternalPlace(
            kakaoPlaceId = kakaoId,
            name = placeName.orEmpty(),
            // 외부(카카오) 원시 분류 → 내부 도메인 카테고리 번역은 어댑터 책임(anti-corruption).
            category = KakaoCategoryMapper.toPlaceCategory(categoryName, categoryGroupCode?.takeIf(String::isNotBlank)),
            categoryDetail = KakaoCategoryMapper.toCategoryDetail(categoryName),
            kakaoCategoryName = categoryName?.takeIf(String::isNotBlank),
            roadAddress = roadAddressName?.takeIf(String::isNotBlank),
            address = addressName?.takeIf(String::isNotBlank),
            coordinate = Coordinate(latitude = latitude, longitude = longitude),
            telephone = phone?.takeIf(String::isNotBlank),
            source = ExternalPlaceSource.KAKAO,
        )
    }
}
