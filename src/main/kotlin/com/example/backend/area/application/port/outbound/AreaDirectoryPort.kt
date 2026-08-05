package com.example.backend.area.application.port.outbound

import com.example.backend.area.application.port.inbound.dto.AreaDescriptor

/**
 * 아웃바운드 포트 — 행정구역 기준정보 조회 계약.
 * 저장소·캐시·계층 파생 방식은 구현체 내부에 숨긴다.
 */
interface AreaDirectoryPort {
    /** 시군구+읍면동 이름 통합 검색(상위 20건, prefix 오름차순). */
    fun search(keyword: String): List<AreaDescriptor>
}
