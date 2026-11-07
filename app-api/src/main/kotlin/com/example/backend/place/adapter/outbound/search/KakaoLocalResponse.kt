package com.example.backend.place.adapter.outbound.search

import com.fasterxml.jackson.annotation.JsonProperty

/** 카카오 로컬 원시 응답 — 어댑터 밖으로 노출하지 않는다(모듈 internal). */
internal data class KakaoLocalResponse(
    val documents: List<KakaoLocalDocument>? = null,
)

internal data class KakaoLocalDocument(
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
