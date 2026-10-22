package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult

/** 인바운드 포트 — 저장된 장소 영업시간과 현재(KST) 영업 상태 조회. */
interface PlaceBusinessHoursQueryUseCase {
    fun getOpeningHours(placeId: Long): PlaceOpeningHoursResult
}
