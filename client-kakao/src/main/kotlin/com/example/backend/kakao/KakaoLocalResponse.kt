package com.example.backend.kakao

import com.fasterxml.jackson.annotation.JsonProperty

/** 카카오 로컬 키워드 검색 원시 응답. 도메인 타입 변환은 이 클라이언트를 쓰는 어댑터가 한다. */
data class KakaoLocalResponse(
    val documents: List<KakaoLocalDocument>? = null,
)

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
