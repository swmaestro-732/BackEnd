package com.example.backend.area.application.port.inbound

import com.example.backend.area.application.port.inbound.dto.AreaDescriptor

/**
 * 인바운드 포트 — 행정구역 조회(공개 API).
 * course·place·user 등 다른 애그리거트는 area 를 객체로 참조하지 않고 코드 값만 보유하며,
 * 행정구역 이름 등이 필요할 때 이 포트로만 조회한다.
 *
 * 시군구 항목은 읍면동 데이터에서 파생되지만, 그 사실은 이 인터페이스에 드러나지 않는다.
 * 코드 단건·계층 탐색 등은 화면 요구가 확정되면 다시 노출한다(우선 검색만).
 */
interface AreaQueryUseCase {
    /** 시군구+읍면동 이름 통합 검색(상위 20건). */
    fun searchAreas(keyword: String): List<AreaDescriptor>

    /**
     * 법정동코드(10자리)로 단건 조회한다 — 읍면동 코드는 읍면동 항목, 시군구 레벨 코드(뒤 5자리 0 패딩)는
     * 시군구 항목을 돌려준다. 코드 표시 이름(코스 area 등)이 필요한 다른 애그리거트가 쓴다. 미존재·비활성이면 null.
     */
    fun findAreaByCode(code: String): AreaDescriptor?
}
