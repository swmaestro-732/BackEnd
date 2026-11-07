package com.example.backend.mobile.course.adapter.inbound.web.response

import com.example.backend.common.domain.CourseTraceStatus
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.application.port.inbound.dto.TracedCourseSummary
import com.example.backend.mobile.course.application.port.inbound.dto.TraceScreenResult
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import java.time.Instant

/**
 * 웹 응답 DTO — 따라가기 상세 화면 조합(BFF). 세션에 코스 요약·코스 장소(캡션·사진·도보 시간)·장소 요약(이름·카테고리·좌표)·체크인 시각을 붙인다.
 * 진행 중이면 따라가기 화면을 다시 그리고, 완주면 기록을 본다(endedAt·소요 시간·거리는 완주일 때 의미 있다).
 * 코스를 더 볼 수 없으면 [course] 의 isDeleted 가 true 이고 제목·썸네일·지역은 null 이지만 [places] 는 그대로 내려준다.
 */
data class TraceScreenResponse(
    val traceId: Long,
    val status: CourseTraceStatus,
    val startedAt: Instant,
    /** COMPLETED 일 때만. */
    val endedAt: Instant?,
    val durationMinutes: Int,
    val distanceMeters: Int,
    val placeCount: Int,
    val visitedPlaceCount: Int,
    val course: TracedCourseScreenResponse,
    val places: List<TracePlaceScreenResponse>,
) {
    companion object {
        fun from(result: TraceScreenResult): TraceScreenResponse {
            val summariesById = result.places.associateBy { it.id }
            val visitedAtByPlace = result.trace.visitedPlaces.associate { it.placeId to it.visitedAt }
            return TraceScreenResponse(
                traceId = result.trace.traceId,
                status = result.trace.status,
                startedAt = result.trace.startedAt,
                endedAt = result.trace.endedAt,
                durationMinutes = result.trace.durationMinutes,
                distanceMeters = result.trace.distanceMeters,
                placeCount = result.trace.places.size,
                visitedPlaceCount = result.trace.visitedPlaces.size,
                course = TracedCourseScreenResponse.from(result.trace.course),
                places =
                    result.trace.places.map {
                        TracePlaceScreenResponse.from(it, summariesById[it.placeId], visitedAtByPlace[it.placeId])
                    },
            )
        }

        /**
         * `?mock=true` 폴백 응답 — 홈 목([com.example.backend.mobile.home.adapter.inbound.web.v2.response.HomeResponse.MOCK])의
         * 따라가는 중 코스(traceId=10)와 맞춰 두었다.
         */
        val MOCK: TraceScreenResponse =
            TraceScreenResponse(
                traceId = 10L,
                status = CourseTraceStatus.IN_PROGRESS,
                startedAt = Instant.parse("2026-11-03T05:30:00Z"),
                endedAt = null,
                durationMinutes = 0,
                distanceMeters = 0,
                placeCount = 2,
                visitedPlaceCount = 1,
                course =
                    TracedCourseScreenResponse(
                        courseId = 3L,
                        title = "을지로 노포 골목 투어",
                        coverImageUrl = "https://cdn.courmy.app/c3.jpg",
                        area = "을지로",
                        isDeleted = false,
                    ),
                places =
                    listOf(
                        TracePlaceScreenResponse(
                            placeId = 101L,
                            orderNo = 0,
                            caption = "점심은 여기서",
                            walkingMinutesToNext = 6,
                            images = listOf(CoursePlaceImageResponse("https://cdn.courmy.app/cp101.jpg", 0)),
                            name = "우래옥",
                            categories = listOf("RESTAURANT"),
                            address = "서울 중구 창경궁로 62-29",
                            imageUrl = "https://cdn.courmy.app/p101.jpg",
                            location = PlaceLocationResponse(latitude = 37.5679, longitude = 126.9992),
                            visitedAt = Instant.parse("2026-11-03T06:10:00Z"),
                        ),
                        TracePlaceScreenResponse(
                            placeId = 205L,
                            orderNo = 1,
                            caption = null,
                            walkingMinutesToNext = null,
                            images = emptyList(),
                            name = "을지다방",
                            categories = listOf("CAFE"),
                            address = "서울 중구 충무로 72-1",
                            imageUrl = null,
                            location = PlaceLocationResponse(latitude = 37.5663, longitude = 126.9927),
                            visitedAt = null,
                        ),
                    ),
            )
    }
}

/** 따라가는 코스 요약 — isDeleted 가 true 면 courseId 외 값은 모두 null 이다. */
data class TracedCourseScreenResponse(
    val courseId: Long,
    val title: String?,
    val coverImageUrl: String?,
    val area: String?,
    val isDeleted: Boolean,
) {
    companion object {
        fun from(course: TracedCourseSummary) =
            TracedCourseScreenResponse(
                courseId = course.courseId,
                title = course.title,
                coverImageUrl = course.coverImageUrl,
                area = course.area,
                isDeleted = course.isDeleted,
            )
    }
}

/**
 * 따라가는 코스의 장소 한 곳(화면용) — 코스 쪽 순서·캡션·사진·도보 시간에 장소 요약과 체크인 시각을 합친 것.
 * 삭제된 장소는 요약이 없어 name·address·location 이 null 이고 categories 는 빈 배열이다(코스 상세 화면과 같은 규칙).
 */
data class TracePlaceScreenResponse(
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    /** 다음 장소까지 도보 이동 시간(분). -1 은 도보 불가, 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResponse>,
    val name: String?,
    val categories: List<String>,
    val address: String?,
    val imageUrl: String?,
    val location: PlaceLocationResponse?,
    /** 체크인 시각. 아직 방문하지 않았으면 null. */
    val visitedAt: Instant?,
) {
    companion object {
        fun from(
            place: CoursePlaceResult,
            summary: PlaceSummary?,
            visitedAt: Instant?,
        ) = TracePlaceScreenResponse(
            placeId = place.placeId,
            orderNo = place.orderNo,
            caption = place.caption,
            walkingMinutesToNext = place.walkingMinutesToNext,
            images = place.images.map(CoursePlaceImageResponse::from),
            name = summary?.name,
            categories = summary?.let { listOf(it.category) } ?: emptyList(),
            address = summary?.address,
            imageUrl = summary?.imageUrl,
            location = summary?.let { PlaceLocationResponse(it.latitude, it.longitude) },
            visitedAt = visitedAt,
        )
    }
}
