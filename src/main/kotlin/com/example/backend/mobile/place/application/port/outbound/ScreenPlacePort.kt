package com.example.backend.mobile.place.application.port.outbound

import com.example.backend.mobile.place.application.port.outbound.dto.ScreenPlace

/**
 * BFF 아웃바운드 포트 — 장소 상세 화면용 단건 조회. 지금은 place 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 place 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface ScreenPlacePort {
    /** [placeId] 장소를 조회한다. 없거나 삭제됐으면 null. */
    fun findById(placeId: Long): ScreenPlace?

    /**
     * [placeIds] 장소들을 한 번에 조회한다 — 목록 화면이 항목별 조회(N+1)를 피하려고 쓴다.
     * 없거나 삭제된 장소는 결과에서 빠지므로 요청 수보다 적을 수 있고, 순서도 보장하지 않는다(호출부가 id 로 매핑).
     */
    fun findByIds(placeIds: List<Long>): List<ScreenPlace>
}
