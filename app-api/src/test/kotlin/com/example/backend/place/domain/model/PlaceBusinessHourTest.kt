package com.example.backend.place.domain.model

import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/** [PlaceBusinessHour] 불변식 — 요일 범위와 개·폐점 시각의 짝. */
class PlaceBusinessHourTest {
    @Test
    fun `영업 요일은 개점과 폐점 시각을 함께 담는다`() {
        val hour = PlaceBusinessHour(dayOfWeek = 1, openTime = LocalTime(9, 0), closeTime = LocalTime(22, 0))

        assertThat(hour.dayOfWeek).isEqualTo(1)
        assertThat(hour.openTime).isEqualTo(LocalTime(9, 0))
        assertThat(hour.closeTime).isEqualTo(LocalTime(22, 0))
    }

    @Test
    fun `휴무는 개점과 폐점이 모두 비어 있다`() {
        val closed = PlaceBusinessHour(dayOfWeek = 7, openTime = null, closeTime = null)

        assertThat(closed.openTime).isNull()
        assertThat(closed.closeTime).isNull()
    }

    @Test
    fun `요일이 1에서 7 밖이면 거부한다`() {
        assertThatThrownBy { PlaceBusinessHour(dayOfWeek = 0, openTime = null, closeTime = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { PlaceBusinessHour(dayOfWeek = 8, openTime = null, closeTime = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `쉬는 시간은 시작과 종료를 함께 담는다`() {
        val hour =
            PlaceBusinessHour(
                dayOfWeek = 2,
                openTime = LocalTime(11, 0),
                closeTime = LocalTime(21, 0),
                breakStart = LocalTime(15, 0),
                breakEnd = LocalTime(17, 0),
            )

        assertThat(hour.breakStart).isEqualTo(LocalTime(15, 0))
        assertThat(hour.breakEnd).isEqualTo(LocalTime(17, 0))
    }

    @Test
    fun `쉬는 시간의 시작과 종료 중 하나만 있으면 거부한다`() {
        assertThatThrownBy {
            PlaceBusinessHour(2, LocalTime(11, 0), LocalTime(21, 0), breakStart = LocalTime(15, 0))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `휴무일에는 쉬는 시간을 둘 수 없다`() {
        assertThatThrownBy {
            PlaceBusinessHour(2, null, null, breakStart = LocalTime(15, 0), breakEnd = LocalTime(17, 0))
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `개점과 폐점 중 하나만 있으면 거부한다`() {
        assertThatThrownBy { PlaceBusinessHour(dayOfWeek = 3, openTime = LocalTime(9, 0), closeTime = null) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { PlaceBusinessHour(dayOfWeek = 3, openTime = null, closeTime = LocalTime(22, 0)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `00시부터 23시 59분까지는 24시간 영업이다`() {
        val allDay = PlaceBusinessHour(dayOfWeek = 3, openTime = LocalTime(0, 0), closeTime = LocalTime(23, 59))
        val regular = PlaceBusinessHour(dayOfWeek = 3, openTime = LocalTime(9, 0), closeTime = LocalTime(23, 59))

        assertThat(allDay.isAllDay).isTrue()
        assertThat(regular.isAllDay).isFalse()
        assertThat(PlaceBusinessHour(dayOfWeek = 3, openTime = null, closeTime = null).isAllDay).isFalse()
    }

    @Test
    fun `폐점이 개점보다 이르면 자정을 넘기는 영업이다`() {
        val lateNight = PlaceBusinessHour(dayOfWeek = 5, openTime = LocalTime(18, 0), closeTime = LocalTime(2, 0))
        val regular = PlaceBusinessHour(dayOfWeek = 5, openTime = LocalTime(9, 0), closeTime = LocalTime(22, 0))

        assertThat(lateNight.crossesMidnight).isTrue()
        assertThat(regular.crossesMidnight).isFalse()
        assertThat(PlaceBusinessHour(dayOfWeek = 5, openTime = null, closeTime = null).crossesMidnight).isFalse()
    }
}
