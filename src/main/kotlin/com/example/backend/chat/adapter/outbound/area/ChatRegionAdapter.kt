package com.example.backend.chat.adapter.outbound.area

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.chat.application.port.outbound.ChatRegionPort
import com.example.backend.common.geo.Coordinate
import org.springframework.stereotype.Component

@Component
class ChatRegionAdapter(
    private val areaQueryUseCase: AreaQueryUseCase,
) : ChatRegionPort {
    override fun findSigunguCodeAt(coordinate: Coordinate): String? = areaQueryUseCase.findSigunguCodeAt(coordinate)

    override fun findAdjacentSigunguCodes(sigunguCode: String): List<String> =
        areaQueryUseCase.findAdjacentSigunguCodes(sigunguCode)
}
