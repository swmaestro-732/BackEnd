package com.example.backend.place.application.port.outbound

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.PlaceBusinessHour

/**
 * 아웃바운드 포트 — 외부 제공자에서 장소의 요일별 영업시간을 찾는다(제공자 중립).
 * null = 호출 실패(다음 동기화에서 재시도), 빈 목록 = 매칭 장소가 없거나 영업시간 정보가 없음(동기화 완료로 기록).
 */
interface BusinessHoursLookupPort {
    fun findBusinessHours(
        name: String,
        address: String,
        coordinate: Coordinate,
    ): List<PlaceBusinessHour>?
}
