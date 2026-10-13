package com.example.backend.mobile.place.application.service

import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult
import kotlinx.datetime.LocalTime

/**
 * 요일별 영업시간을 월요일부터 한 줄씩 줄바꿈으로 이은 문구로 만든다. 정보가 없으면 null.
 * 예: "월 10:00 - 22:00 (브레이크타임 15:00 - 17:00)\n…\n일 휴무".
 */
internal object PlaceOpeningHoursText {
    private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

    fun of(hours: List<PlaceOpeningHoursResult.Hour>): String? {
        if (hours.isEmpty()) return null
        return hours.sortedBy { it.dayOfWeek }.joinToString("\n") { "${DAY_NAMES[it.dayOfWeek - 1]} ${it.toText()}" }
    }

    private fun PlaceOpeningHoursResult.Hour.toText(): String {
        val open = openTime ?: return "휴무"
        if (allDay) return "24시간 영업"
        val range = "${open.format()} - ${closeTime!!.format()}"
        val breakRange = breakStart?.let { " (브레이크타임 ${it.format()} - ${breakEnd!!.format()})" }.orEmpty()
        return range + breakRange
    }

    private fun LocalTime.format(): String = "%02d:%02d".format(hour, minute)
}
