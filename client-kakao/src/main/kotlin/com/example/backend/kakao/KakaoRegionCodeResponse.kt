package com.example.backend.kakao

import com.fasterxml.jackson.annotation.JsonProperty

/** 카카오 좌표→행정구역 변환 원시 응답. 법정동 선택은 이 클라이언트를 쓰는 어댑터가 한다. */
data class KakaoRegionCodeResponse(
    val documents: List<KakaoRegionCodeDocument>? = null,
)

data class KakaoRegionCodeDocument(
    /** "B"=법정동, "H"=행정동. 법정동코드는 B 문서에서 읽는다. */
    @param:JsonProperty("region_type")
    val regionType: String? = null,
    /** 법정동/행정동 코드 10자리. */
    val code: String? = null,
)
