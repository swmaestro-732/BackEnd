package com.example.backend.place.domain.model

import kotlinx.datetime.LocalTime

/**
 * 요일별 영업시간. dayOfWeek 는 ISO(1=월 … 7=일). 휴무는 open/close 모두 null.
 * 자정을 넘기는 영업(예: 18:00~02:00)은 closeTime 이 openTime 보다 이른 값으로 담긴다(다음 날 종료).
 * 브레이크타임이 있으면 영업은 첫 개점~마지막 폐점으로 담고 쉬는 구간을 [breakStart]~[breakEnd] 에 담는다.
 */
data class PlaceBusinessHour(
    val dayOfWeek: Int,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val breakStart: LocalTime? = null,
    val breakEnd: LocalTime? = null,
) {
    init {
        require(dayOfWeek in 1..7) { "요일은 1~7 이어야 합니다: $dayOfWeek" }
        require((openTime == null) == (closeTime == null)) { "개점·폐점 시각은 함께 있거나 함께 없어야 합니다." }
        require((breakStart == null) == (breakEnd == null)) { "쉬는 시간의 시작·종료는 함께 있거나 함께 없어야 합니다." }
        require(breakStart == null || openTime != null) { "휴무일에는 쉬는 시간을 둘 수 없습니다." }
    }

    /** 24시간 영업 — 00:00~23:59 로 저장된다. */
    val isAllDay: Boolean
        get() = openTime == DAY_START && closeTime == DAY_END

    val crossesMidnight: Boolean
        get() = openTime != null && closeTime!! < openTime

    private companion object {
        val DAY_START = LocalTime(0, 0)
        val DAY_END = LocalTime(23, 59)
    }
}
