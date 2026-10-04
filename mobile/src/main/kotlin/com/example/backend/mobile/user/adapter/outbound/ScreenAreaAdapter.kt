package com.example.backend.mobile.user.adapter.outbound

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.mobile.user.application.port.outbound.ScreenAreaPort
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 지역 이름 해석을 area 도메인 인바운드 포트에 위임한다.
 * (지금은 인프로세스 위임. MSA 분리 시 이 어댑터만 area 서비스 HTTP 클라이언트로 교체한다.)
 */
@Component
class ScreenAreaAdapter(
    private val areaQueryUseCase: AreaQueryUseCase,
) : ScreenAreaPort {
    override fun findAreaName(code: String): String? = areaQueryUseCase.findAreaByCode(code)?.shortName
}
