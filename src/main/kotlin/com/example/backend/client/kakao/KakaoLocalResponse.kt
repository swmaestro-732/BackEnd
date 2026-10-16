package com.example.backend.client.kakao

import com.fasterxml.jackson.annotation.JsonProperty

/** 카카오 로컬 원시 응답 — 클라이언트 밖으로 노출하지 않는다(모듈 internal). */
internal data class KakaoLocalResponse(
    val documents: List<KakaoLocalDocument>? = null,
)

/** 카카오 로컬 검색 결과 문서 — 도메인 변환은 사용하는 쪽 어댑터 책임. */
data class KakaoLocalDocument(
    @param:JsonProperty("id")
    val id: String? = null,
    @param:JsonProperty("place_name")
    val placeName: String? = null,
    @param:JsonProperty("category_name")
    val categoryName: String? = null,
    @param:JsonProperty("category_group_code")
    val categoryGroupCode: String? = null,
    @param:JsonProperty("road_address_name")
    val roadAddressName: String? = null,
    @param:JsonProperty("address_name")
    val addressName: String? = null,
    val x: String? = null,
    val y: String? = null,
    val phone: String? = null,
)
