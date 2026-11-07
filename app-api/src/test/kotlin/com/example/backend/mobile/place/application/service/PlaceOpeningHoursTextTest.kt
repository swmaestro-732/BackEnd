package com.example.backend.mobile.place.application.service

import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [PlaceOpeningHoursText] — 요일별 한 줄 문구. 휴무·24시간·브레이크타임 표기와 요일 정렬을 검증한다. */
class PlaceOpeningHoursTextTest {
    @Test
    fun `정보가 없으면 null`() {
        assertThat(PlaceOpeningHoursText.of(emptyList())).isNull()
    }

    @Test
    fun `요일 순으로 한 줄씩 잇고 휴무·24시간·브레이크타임을 표기한다`() {
        val text =
            PlaceOpeningHoursText.of(
                listOf(
                    hour(7, null, null),
                    hour(2, LocalTime(0, 0), LocalTime(23, 59), allDay = true),
                    hour(1, LocalTime(9, 30), LocalTime(22, 0), LocalTime(15, 0), LocalTime(17, 0)),
                    hour(3, LocalTime(18, 0), LocalTime(2, 0)),
                ),
            )

        assertThat(text).isEqualTo(
            listOf(
                "월 09:30 - 22:00 (브레이크타임 15:00 - 17:00)",
                "화 24시간 영업",
                "수 18:00 - 02:00",
                "일 휴무",
            ).joinToString("\n"),
        )
    }

    private fun hour(
        day: Int,
        open: LocalTime?,
        close: LocalTime?,
        breakStart: LocalTime? = null,
        breakEnd: LocalTime? = null,
        allDay: Boolean = false,
    ) = PlaceOpeningHoursResult.Hour(day, open, close, breakStart, breakEnd, allDay)
}
