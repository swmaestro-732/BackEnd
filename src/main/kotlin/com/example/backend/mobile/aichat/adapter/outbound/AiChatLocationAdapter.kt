package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.aichat.application.port.outbound.AiChatLocationPort
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — "내 근처" 좌표를 법정동코드로 바꾸는 일을 place 도메인 인바운드 포트에 위임한다
 * (근처 장소의 법정동코드 다수결). 엔진이 없거나(503) 근처에 장소가 없으면 null — 호출자가 지역 칩을 다시 안내한다.
 */
@Component
class AiChatLocationAdapter(
    private val placeQueryUseCase: PlaceQueryUseCase,
) : AiChatLocationPort {
    private val log = KotlinLogging.logger {}

    override fun findAreaCodeAt(location: Coordinate): String? =
        try {
            placeQueryUseCase.findDominantAreaCodeNear(location)
        } catch (e: BusinessException) {
            log.warn { "ai-chat 좌표 → 지역 판정 실패(지역 재안내): ${e.message}" }
            null
        }
}
