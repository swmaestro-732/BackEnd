package com.example.backend.place.domain.model

import com.example.backend.common.geo.Coordinate

/**
 * 외부 지도 제공자(네이버·카카오)에서 검색한 장소. 내부 목 장소([com.example.backend.place ... MockPlace])와 달리
 * 저장소 식별자(id)가 없고, 어떤 제공자에서 왔는지([source])를 함께 들고 다닌다.
 */
data class ExternalPlace(
    val name: String,
    val category: String,
    val roadAddress: String?,
    val address: String?,
    val coordinate: Coordinate,
    val telephone: String?,
    val source: ExternalPlaceSource,
)

/** 외부 장소의 출처 제공자. */
enum class ExternalPlaceSource {
    NAVER,
    KAKAO,
}
