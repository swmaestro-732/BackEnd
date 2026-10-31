package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult
import com.example.backend.place.application.port.outbound.PlaceBusinessHourQueryPort
import com.example.backend.place.domain.model.PlaceBusinessHour
import kotlinx.datetime.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * [PlaceBusinessHoursQueryService] — 저장된 행을 DTO 로 옮기고 현재 시각 기준 상태를 붙인다.
 * 현재 시각에 의존하므로 시각과 무관하게 결과가 정해지는 입력(정보 없음·매일 24시간)으로 검증한다.
 */
class PlaceBusinessHoursQueryServiceTest {
    @Test
    fun `영업시간 정보가 없으면 UNKNOWN 과 빈 목록`() {
        val service = PlaceBusinessHoursQueryService { emptyList() }

        val result = service.getOpeningHours(1L)

        assertThat(result.openStatus).isEqualTo("UNKNOWN")
        assertThat(result.hours).isEmpty()
    }

    @Test
    fun `행을 요일별 DTO 로 옮기고 24시간 여부를 표시한다`() {
        val service =
            PlaceBusinessHoursQueryService {
                (1..7).map { day ->
                    if (day == 7) {
                        PlaceBusinessHour(day, LocalTime(0, 0), LocalTime(23, 59))
                    } else {
                        PlaceBusinessHour(day, LocalTime(0, 0), LocalTime(23, 59), LocalTime(3, 0), LocalTime(4, 0))
                    }
                }
            }

        val result = service.getOpeningHours(1L)

        assertThat(result.hours).hasSize(7)
        assertThat(result.hours.map { it.allDay }).containsOnly(true)
        assertThat(result.hours.single { it.dayOfWeek == 7 }).isEqualTo(
            PlaceOpeningHoursResult.Hour(7, LocalTime(0, 0), LocalTime(23, 59), null, null, allDay = true),
        )
        assertThat(result.hours.single { it.dayOfWeek == 1 }.breakStart).isEqualTo(LocalTime(3, 0))
        assertThat(result.hours.single { it.dayOfWeek == 1 }.breakEnd).isEqualTo(LocalTime(4, 0))
        // 매일 24시간이라 어느 시각에 돌려도 OPEN 또는 BREAK — UNKNOWN·CLOSED 는 아니다
        assertThat(result.openStatus).isIn("OPEN", "BREAK")
    }

    private fun PlaceBusinessHoursQueryService(findByPlaceId: (Long) -> List<PlaceBusinessHour>) =
        PlaceBusinessHoursQueryService(
            object : PlaceBusinessHourQueryPort {
                override fun findByPlaceId(placeId: Long): List<PlaceBusinessHour> = findByPlaceId(placeId)
            },
        )
}
