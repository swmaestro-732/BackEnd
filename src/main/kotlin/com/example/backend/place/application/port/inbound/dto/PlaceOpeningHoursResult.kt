package com.example.backend.place.application.port.inbound.dto

import kotlinx.datetime.LocalTime

/**
 * 인바운드 포트 반환 DTO — 현재 영업 상태([com.example.backend.place.domain.model.PlaceOpenStatus] 이름)와 요일별 영업시간.
 * 영업시간 정보가 없으면 [hours] 는 빈 목록이고 [openStatus] 는 UNKNOWN 이다.
 */
data class PlaceOpeningHoursResult(
    val openStatus: String,
    val hours: List<Hour>,
) {
    /** 요일(ISO 1=월 … 7=일)별 영업시간. 휴무는 open/close 가 null, [allDay] 는 24시간 영업. */
    data class Hour(
        val dayOfWeek: Int,
        val openTime: LocalTime?,
        val closeTime: LocalTime?,
        val breakStart: LocalTime?,
        val breakEnd: LocalTime?,
        val allDay: Boolean,
    )
}
