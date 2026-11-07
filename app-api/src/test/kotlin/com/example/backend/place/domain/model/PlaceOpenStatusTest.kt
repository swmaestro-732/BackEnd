package com.example.backend.place.domain.model

import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** [PlaceOpenStatus.of] — 요일·시각 기준 판정. 휴무·24시간·브레이크타임·자정 넘김(전날 이어짐)을 가른다. */
class PlaceOpenStatusTest {
    private val weekday = PlaceBusinessHour(1, LocalTime(10, 0), LocalTime(22, 0), LocalTime(15, 0), LocalTime(17, 0))
    private val closedSunday = PlaceBusinessHour(7, null, null)
    private val lateNightFriday = PlaceBusinessHour(5, LocalTime(18, 0), LocalTime(2, 0))
    private val allDaySaturday = PlaceBusinessHour(6, LocalTime(0, 0), LocalTime(23, 59))

    @Test
    fun `영업시간 정보가 없으면 UNKNOWN`() {
        assertThat(PlaceOpenStatus.of(emptyList(), 1, LocalTime(12, 0))).isEqualTo(PlaceOpenStatus.UNKNOWN)
    }

    @Test
    fun `영업 중이면 OPEN, 쉬는 시간이면 BREAK, 영업 전후면 CLOSED`() {
        val hours = listOf(weekday)

        assertThat(PlaceOpenStatus.of(hours, 1, LocalTime(12, 0))).isEqualTo(PlaceOpenStatus.OPEN)
        assertThat(PlaceOpenStatus.of(hours, 1, LocalTime(16, 0))).isEqualTo(PlaceOpenStatus.BREAK)
        assertThat(PlaceOpenStatus.of(hours, 1, LocalTime(9, 59))).isEqualTo(PlaceOpenStatus.CLOSED)
        assertThat(PlaceOpenStatus.of(hours, 1, LocalTime(22, 0))).isEqualTo(PlaceOpenStatus.CLOSED)
    }

    @Test
    fun `행이 없는 요일과 휴무 요일은 CLOSED`() {
        assertThat(PlaceOpenStatus.of(listOf(weekday), 3, LocalTime(12, 0))).isEqualTo(PlaceOpenStatus.CLOSED)
        assertThat(PlaceOpenStatus.of(listOf(closedSunday), 7, LocalTime(12, 0))).isEqualTo(PlaceOpenStatus.CLOSED)
    }

    @Test
    fun `24시간 영업은 어느 시각이든 OPEN`() {
        assertThat(PlaceOpenStatus.of(listOf(allDaySaturday), 6, LocalTime(3, 30))).isEqualTo(PlaceOpenStatus.OPEN)
    }

    @Test
    fun `자정을 넘기는 영업은 개점 후면 OPEN, 개점 전이면 CLOSED`() {
        val hours = listOf(lateNightFriday)

        assertThat(PlaceOpenStatus.of(hours, 5, LocalTime(23, 30))).isEqualTo(PlaceOpenStatus.OPEN)
        assertThat(PlaceOpenStatus.of(hours, 5, LocalTime(17, 0))).isEqualTo(PlaceOpenStatus.CLOSED)
    }

    @Test
    fun `전날 영업이 자정을 넘기면 다음 날 새벽도 이어서 OPEN`() {
        val hours = listOf(lateNightFriday)

        // 토요일 01:00 — 금요일 18:00~02:00 이 아직 영업 중 (토요일 행은 없어도)
        assertThat(PlaceOpenStatus.of(hours, 6, LocalTime(1, 0))).isEqualTo(PlaceOpenStatus.OPEN)
        // 토요일 02:00 — 전날 영업 종료, 토요일 행 없음
        assertThat(PlaceOpenStatus.of(hours, 6, LocalTime(2, 0))).isEqualTo(PlaceOpenStatus.CLOSED)
    }

    @Test
    fun `월요일의 전날은 일요일이다`() {
        val sundayLateNight = PlaceBusinessHour(7, LocalTime(20, 0), LocalTime(1, 0))

        assertThat(PlaceOpenStatus.of(listOf(sundayLateNight), 1, LocalTime(0, 30))).isEqualTo(PlaceOpenStatus.OPEN)
    }
}
