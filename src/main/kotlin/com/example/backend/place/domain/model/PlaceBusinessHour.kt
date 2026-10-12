package com.example.backend.place.domain.model

import kotlinx.datetime.LocalTime

/**
 * 요일별 영업시간. dayOfWeek 는 ISO(1=월 … 7=일). 휴무는 open/close 모두 null.
 * 자정을 넘기는 영업(예: 18:00~02:00)은 closeTime 이 openTime 보다 이른 값으로 담긴다(다음 날 종료).
 */
data class PlaceBusinessHour(
    val dayOfWeek: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
) {
    init {
        require(dayOfWeek in 1..7) { "요일은 1~7 이어야 합니다: $dayOfWeek" }
        require((openTime == null) == (closeTime == null)) { "개점·폐점 시각은 함께 있거나 함께 없어야 합니다." }
    }
}
