package com.example.backend.place.domain.model

import kotlinx.datetime.LocalTime

/** 특정 시각의 영업 여부. 영업시간 정보가 없으면 UNKNOWN. */
enum class PlaceOpenStatus {
    OPEN,
    BREAK,
    CLOSED,
    UNKNOWN,
    ;

    companion object {
        /** [dayOfWeek](ISO 1=월 … 7=일)의 [time] 기준 상태. 자정을 넘긴 전날 영업도 이어서 본다. */
        fun of(
            hours: List<PlaceBusinessHour>,
            dayOfWeek: Int,
            time: LocalTime,
        ): PlaceOpenStatus {
            if (hours.isEmpty()) return UNKNOWN
            val previousDay = if (dayOfWeek == 1) 7 else dayOfWeek - 1
            val yesterday = hours.find { it.dayOfWeek == previousDay }
            if (yesterday != null && yesterday.crossesMidnight && time < yesterday.closeTime!!) return OPEN

            val today = hours.find { it.dayOfWeek == dayOfWeek } ?: return CLOSED
            val open = today.openTime ?: return CLOSED
            val opened =
                when {
                    today.isAllDay -> true
                    today.crossesMidnight -> time >= open
                    else -> time >= open && time < today.closeTime!!
                }
            if (!opened) return CLOSED
            val inBreak = today.breakStart != null && time >= today.breakStart && time < today.breakEnd!!
            return if (inBreak) BREAK else OPEN
        }
    }
}
