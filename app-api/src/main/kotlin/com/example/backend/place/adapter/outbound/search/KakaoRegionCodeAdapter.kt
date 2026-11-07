package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.kakao.KakaoLocalClient
import com.example.backend.place.application.port.outbound.AreaCodeLookupPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 카카오 좌표→행정구역(coord2regioncode) 어댑터. 호출은 [KakaoLocalClient] 가 한다.
 *
 * 응답 `documents[]` 중 법정동(region_type="B") 문서의 code(법정동코드 10자리)를 돌려준다.
 * 저장 흐름의 부가 정보 조회라 실패하면(키 미설정으로 인한 401 포함) null 을 돌려준다(fail-soft).
 */
@Component
class KakaoRegionCodeAdapter(
    private val kakaoLocalClient: KakaoLocalClient,
) : AreaCodeLookupPort {
    private val log = KotlinLogging.logger {}

    override fun findLegalDongCode(coordinate: Coordinate): String? =
        try {
            val response =
                kakaoLocalClient.coordToRegionCode(
                    longitude = coordinate.longitude,
                    latitude = coordinate.latitude,
                )
            response
                ?.documents
                .orEmpty()
                .firstOrNull { it.regionType == LEGAL_DONG_TYPE }
                ?.code
                ?.takeIf { it.matches(LEGAL_DONG_CODE_PATTERN) }
        } catch (exception: Exception) {
            log.warn(exception) { "카카오 좌표→법정동 변환 호출 실패: coordinate=$coordinate" }
            null
        }

    private companion object {
        const val LEGAL_DONG_TYPE = "B"
        val LEGAL_DONG_CODE_PATTERN = Regex("^[0-9]{10}$")
    }
}
