package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate

/**
 * 아웃바운드 포트 — 좌표로 법정동코드(10자리)를 조회한다(제공자 중립).
 * 장소 저장 시 부가 정보(enrichment)라 실패해도 저장을 막지 않는다: 호출 실패·미확인 시 null(fail-soft).
 */
interface AreaCodeLookupPort {
    fun findLegalDongCode(coordinate: Coordinate): String?
}
