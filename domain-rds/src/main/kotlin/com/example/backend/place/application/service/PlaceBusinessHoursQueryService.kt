package com.example.backend.place.application.service

import com.example.backend.place.application.port.inbound.PlaceBusinessHoursQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceOpeningHoursResult
import com.example.backend.place.application.port.outbound.PlaceBusinessHourQueryPort
import com.example.backend.place.domain.model.PlaceOpenStatus
import kotlinx.datetime.toKotlinLocalTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

/** 장소 영업시간 조회. 영업 상태는 한국 시각 기준으로 판정한다. */
@Service
@Transactional(readOnly = true)
class PlaceBusinessHoursQueryService(
    private val queryPort: PlaceBusinessHourQueryPort,
) : PlaceBusinessHoursQueryUseCase {
    override fun getOpeningHours(placeId: Long): PlaceOpeningHoursResult {
        val hours = queryPort.findByPlaceId(placeId)
        val now = LocalDateTime.now(KST)
        val status = PlaceOpenStatus.of(hours, now.dayOfWeek.value, now.toLocalTime().toKotlinLocalTime())
        return PlaceOpeningHoursResult(
            openStatus = status.name,
            hours =
                hours.map {
                    PlaceOpeningHoursResult.Hour(
                        dayOfWeek = it.dayOfWeek,
                        openTime = it.openTime,
                        closeTime = it.closeTime,
                        breakStart = it.breakStart,
                        breakEnd = it.breakEnd,
                        allDay = it.isAllDay,
                    )
                },
        )
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
