package com.example.backend.mobile.place.application.port.outbound

import com.example.backend.mobile.place.application.port.outbound.dto.ScreenPlace

/**
 * BFF 아웃바운드 포트 — 장소 상세 화면용 단건 조회. 지금은 place 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 place 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface ScreenPlacePort {
    /** [placeId] 장소를 조회한다. 없거나 삭제됐으면 null. */
    fun findById(placeId: Long): ScreenPlace?
}
