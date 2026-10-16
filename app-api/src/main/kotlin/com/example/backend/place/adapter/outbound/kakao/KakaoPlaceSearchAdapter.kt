package com.example.backend.place.adapter.outbound.kakao

import com.example.backend.client.kakao.KakaoLocalDocument
import com.example.backend.client.kakao.KakaoLocalSearchClient
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.ExternalPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import org.springframework.stereotype.Component

/** 카카오 로컬 검색 결과 → [ExternalPlace]. 식별자·좌표가 없는 문서는 버린다. */
@Component
class KakaoPlaceSearchAdapter(
    private val kakaoLocalSearchClient: KakaoLocalSearchClient,
) : ExternalPlaceSearchPort {
    override fun search(
        query: String,
        near: Coordinate?,
    ): List<ExternalPlace> = kakaoLocalSearchClient.search(query, near).mapNotNull { it.toExternalPlace() }

    private fun KakaoLocalDocument.toExternalPlace(): ExternalPlace? {
        val kakaoId = id?.takeIf(String::isNotBlank) ?: return null
        val longitude = x?.toDoubleOrNull() ?: return null
        val latitude = y?.toDoubleOrNull() ?: return null
        return ExternalPlace(
            kakaoPlaceId = kakaoId,
            name = placeName.orEmpty(),
            // 외부(카카오) 원시 분류 → 내부 도메인 카테고리 번역은 어댑터 책임(anti-corruption).
            category = KakaoCategoryMapper.toPlaceCategory(categoryGroupCode?.takeIf(String::isNotBlank)),
            roadAddress = roadAddressName?.takeIf(String::isNotBlank),
            address = addressName?.takeIf(String::isNotBlank),
            coordinate = Coordinate(latitude = latitude, longitude = longitude),
            telephone = phone?.takeIf(String::isNotBlank),
            source = ExternalPlaceSource.KAKAO,
        )
    }
}
