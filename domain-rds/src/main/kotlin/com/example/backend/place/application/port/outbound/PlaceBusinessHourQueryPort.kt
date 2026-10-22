package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceBusinessHour

/** 장소 영업시간 조회 아웃바운드 포트. */
interface PlaceBusinessHourQueryPort {
    /** 장소의 요일별 영업시간(요일순). 미동기화이거나 정보가 없으면 빈 목록. */
    fun findByPlaceId(placeId: Long): List<PlaceBusinessHour>
}
