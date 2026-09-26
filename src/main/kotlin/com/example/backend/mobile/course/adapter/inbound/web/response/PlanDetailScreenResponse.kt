package com.example.backend.mobile.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.PlanPlaceResult
import com.example.backend.mobile.course.application.port.inbound.dto.PlanDetailScreenResult
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import java.time.Instant
import java.time.LocalDate

/**
 * 웹 응답 DTO — 계획 상세 화면 조합(BFF). 프론트 화면 계약 형태.
 * 계획 상세([com.example.backend.course.application.port.inbound.dto.PlanDetailResult])에
 * 장소 요약([PlaceSummary])을 붙여 채운다 — 도메인 API(`GET /api/v1/plans/{planId}`)는 placeId 만 주므로
 * 지도 핀·장소 카드를 그리려면 이 API 를 쓴다.
 */
data class PlanDetailScreenResponse(
    val planId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    /** 계획 복제 원본 코스 id. 자유 계획이면 null. */
    val sourceCourseId: Long?,
    val placeCount: Int,
    /** 구간 도보 시간 합(분). 도보 불가(-1) 구간은 소요 시간이 아니라 합계에서 뺀다. */
    val walkingMinutes: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val places: List<PlanPlaceScreenResponse>,
) {
    companion object {
        fun from(result: PlanDetailScreenResult): PlanDetailScreenResponse {
            val placesById = result.places.associateBy { it.id }
            val plan = result.plan
            return PlanDetailScreenResponse(
                planId = plan.id,
                title = plan.title,
                memo = plan.memo,
                plannedDate = plan.plannedDate,
                sourceCourseId = plan.sourceCourseId,
                placeCount = plan.places.size,
                walkingMinutes =
                    plan.places
                        .mapNotNull { it.walkingMinutesToNext }
                        .filter { it > 0 }
                        .sum(),
                createdAt = plan.createdAt,
                updatedAt = plan.updatedAt,
                places = plan.places.map { PlanPlaceScreenResponse.from(it, placesById[it.placeId]) },
            )
        }

        /**
         * `?mock=true` 폴백 응답 — 시드/DB 없이 프론트가 붙어볼 수 있게 고정 화면 목을 내려준다.
         * 계획 상세 목([com.example.backend.course.adapter.inbound.web.response.PlanDetailResponse.MOCK], planId=1)과
         * 같은 계획으로 값을 맞춰 두었다.
         */
        val MOCK: PlanDetailScreenResponse =
            PlanDetailScreenResponse(
                planId = 1L,
                title = "토요일 성수 데이트",
                memo = "3시 전엔 출발하기",
                plannedDate = LocalDate.parse("2026-09-20"),
                sourceCourseId = 12L,
                placeCount = 2,
                walkingMinutes = 6,
                createdAt = Instant.parse("2026-09-18T07:00:00Z"),
                updatedAt = Instant.parse("2026-09-18T08:00:00Z"),
                places =
                    listOf(
                        PlanPlaceScreenResponse(
                            placeId = 101L,
                            orderNo = 0,
                            memo = "웨이팅 있으면 옆집으로",
                            walkingMinutesToNext = 6,
                            name = "어니언 성수",
                            categories = listOf("CAFE"),
                            address = "서울 성동구 아차산로9길 8",
                            imageUrl = "https://cdn.courmy.app/p101.jpg",
                            location = PlaceLocationResponse(latitude = 37.5445, longitude = 127.0575),
                        ),
                        PlanPlaceScreenResponse(
                            placeId = 205L,
                            orderNo = 1,
                            memo = null,
                            walkingMinutesToNext = null,
                            name = "대림창고 갤러리",
                            categories = listOf("CAFE"),
                            address = "서울 성동구 성수이로 78",
                            imageUrl = null,
                            location = PlaceLocationResponse(latitude = 37.5419, longitude = 127.0555),
                        ),
                    ),
            )
    }
}

/**
 * 계획에 담긴 장소 한 곳(화면용) — 계획 레코드의 순서·메모에 장소 요약을 합친 것.
 * 삭제된 장소는 요약이 없어 [name]·[location] 등이 null 이고 [categories] 는 빈 배열이다
 * (코스 상세 화면의 장소 계약과 같은 규칙).
 */
data class PlanPlaceScreenResponse(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
    /** 다음 장소까지 도보 이동 시간(분). -1 은 도보 불가, 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val name: String?,
    val categories: List<String>,
    val address: String?,
    val imageUrl: String?,
    val location: PlaceLocationResponse?,
) {
    companion object {
        fun from(
            place: PlanPlaceResult,
            summary: PlaceSummary?,
        ): PlanPlaceScreenResponse =
            PlanPlaceScreenResponse(
                placeId = place.placeId,
                orderNo = place.orderNo,
                memo = place.memo,
                walkingMinutesToNext = place.walkingMinutesToNext,
                name = summary?.name,
                categories = summary?.let { listOf(it.category) } ?: emptyList(),
                address = summary?.address,
                imageUrl = summary?.imageUrl,
                location = summary?.let { PlaceLocationResponse(it.latitude, it.longitude) },
            )
    }
}
