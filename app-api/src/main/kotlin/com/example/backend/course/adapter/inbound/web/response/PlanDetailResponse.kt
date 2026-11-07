package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.PlanDetailResult
import com.example.backend.course.application.port.inbound.dto.PlanPlaceResult
import java.time.Instant
import java.time.LocalDate

/** 계획 상세(노션 명세 · Course · 계획 상세 조회). 장소는 placeId·순서·메모만 — 이름·좌표는 BFF 가 붙인다. */
data class PlanDetailResponse(
    val planId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    val sourceCourseId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val places: List<PlanPlaceResponse>,
) {
    companion object {
        fun from(result: PlanDetailResult) =
            PlanDetailResponse(
                planId = result.id,
                title = result.title,
                memo = result.memo,
                plannedDate = result.plannedDate,
                sourceCourseId = result.sourceCourseId,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
                places = result.places.map(PlanPlaceResponse::from),
            )

        /** `?mock=true` 폴백 고정 목(planId=1). */
        val MOCK =
            PlanDetailResponse(
                planId = 1L,
                title = "토요일 성수 데이트",
                memo = "3시 전엔 출발하기",
                plannedDate = LocalDate.parse("2026-09-20"),
                sourceCourseId = 12L,
                createdAt = Instant.parse("2026-09-18T07:00:00Z"),
                updatedAt = Instant.parse("2026-09-18T08:00:00Z"),
                places =
                    listOf(
                        PlanPlaceResponse(
                            placeId = 101L,
                            orderNo = 0,
                            memo = "웨이팅 있으면 옆집으로",
                            walkingMinutesToNext = 6,
                        ),
                        PlanPlaceResponse(placeId = 205L, orderNo = 1, memo = null, walkingMinutesToNext = null),
                    ),
            )
    }
}

data class PlanPlaceResponse(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
    /** 다음 장소까지 도보 이동 시간(분). -1 은 도보 불가, 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
) {
    companion object {
        fun from(place: PlanPlaceResult) =
            PlanPlaceResponse(
                placeId = place.placeId,
                orderNo = place.orderNo,
                memo = place.memo,
                walkingMinutesToNext = place.walkingMinutesToNext,
            )
    }
}
