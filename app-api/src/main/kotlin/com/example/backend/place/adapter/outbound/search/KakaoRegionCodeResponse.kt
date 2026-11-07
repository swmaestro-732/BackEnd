package com.example.backend.place.adapter.outbound.search

import com.fasterxml.jackson.annotation.JsonProperty

/** 카카오 좌표→행정구역 변환 원시 응답 — 어댑터 밖으로 노출하지 않는다(모듈 internal). */
internal data class KakaoRegionCodeResponse(
    val documents: List<KakaoRegionCodeDocument>? = null,
)

internal data class KakaoRegionCodeDocument(
    /** "B"=법정동, "H"=행정동. 법정동코드는 B 문서에서 읽는다. */
    @param:JsonProperty("region_type")
    val regionType: String? = null,
    /** 법정동/행정동 코드 10자리. */
    val code: String? = null,
)
