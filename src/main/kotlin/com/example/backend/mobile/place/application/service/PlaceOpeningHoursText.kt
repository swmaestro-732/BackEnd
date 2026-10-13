package com.example.backend.mobile.place.application.service

import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult
import kotlinx.datetime.LocalTime

/**
 * 요일별 영업시간을 주간 요약 문구로 만든다. 같은 시간대 요일을 묶는다.
 * 예: "매일 11:00 – 21:00", "월–금 11:00 – 21:00 (브레이크 15:00 – 17:00), 토·일 휴무". 정보가 없으면 null.
 */
internal object PlaceOpeningHoursText {
    private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

    fun of(hours: List<PlaceOpeningHoursResult.Hour>): String? {
        if (hours.isEmpty()) return null
        val groups = hours.sortedBy { it.dayOfWeek }.groupBy { it.copy(dayOfWeek = 0) }
        if (groups.size == 1 && hours.size == DAY_NAMES.size) return "매일 ${groups.keys.single().toText()}"
        return groups.entries.joinToString(", ") { (hour, days) ->
            "${dayLabel(days.map { it.dayOfWeek })} ${hour.toText()}"
        }
    }

    private fun PlaceOpeningHoursResult.Hour.toText(): String {
        val open = openTime ?: return "휴무"
        if (allDay) return "24시간 영업"
        val range = "${open.format()} – ${closeTime!!.format()}"
        val breakRange = breakStart?.let { " (브레이크 ${it.format()} – ${breakEnd!!.format()})" }.orEmpty()
        return range + breakRange
    }

    /** 연속 요일은 3일 이상이면 "월–수", 2일이면 "월·화"로 묶는다. */
    private fun dayLabel(days: List<Int>): String {
        val runs = mutableListOf<MutableList<Int>>()
        days.forEach { day ->
            if (runs.lastOrNull()?.last() == day - 1) runs.last().add(day) else runs.add(mutableListOf(day))
        }
        return runs.joinToString("·") { run ->
            if (run.size >=
                3
            ) {
                "${DAY_NAMES[run.first() - 1]}–${DAY_NAMES[run.last() - 1]}"
            } else {
                run.joinToString("·") {
                    DAY_NAMES[
                        it -
                            1,
                    ]
                }
            }
        }
    }

    private fun LocalTime.format(): String = "%02d:%02d".format(hour, minute)
}
