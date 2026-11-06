package com.example.backend.chat.application.port.outbound

import com.example.backend.common.geo.Coordinate

/** chat 코어는 area 를 직접 알지 않고 ACL 어댑터가 area 인바운드 포트에 위임한다. */
interface ChatRegionPort {
    fun findSigunguCodeAt(coordinate: Coordinate): String?

    fun findAdjacentSigunguCodes(sigunguCode: String): List<String>
}
