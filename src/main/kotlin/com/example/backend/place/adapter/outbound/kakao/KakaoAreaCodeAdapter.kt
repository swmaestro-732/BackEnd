package com.example.backend.place.adapter.outbound.kakao

import com.example.backend.client.kakao.KakaoRegionCodeClient
import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import org.springframework.stereotype.Component

@Component
class KakaoAreaCodeAdapter(
    private val kakaoRegionCodeClient: KakaoRegionCodeClient,
) : AreaCodeLookupPort {
    override fun findLegalDongCode(coordinate: Coordinate): String? =
        kakaoRegionCodeClient.findLegalDongCode(coordinate)
}
