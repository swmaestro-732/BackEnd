package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceImageResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult

/**
 * 웹 응답 DTO — 코스 상세. 프론트 계약대로 최상위를 { "course": {...} } 로 감싼다
 * (공통 ApiResponse.data 안에 들어간다). id 는 외부 계약상 문자열로 다룬다.
 *
 * 표시 로직(도보 시간 합계·팔로우/따라가기 축약 라벨)은 이 계층의 [from] 매퍼가 담당한다 —
 * 애플리케이션 결과([CourseDetailResult])는 원시값만 넘겨준다.
 */
data class CourseDetailResponse(
    val course: CourseResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseDetailResponse = CourseDetailResponse(CourseResponse.from(result))

        private fun image(token: String) = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc$token&s=10"

        private fun img(
            token: String,
            orderNo: Int,
        ) = CoursePlaceImageResponse(imageUrl = image(token), orderNo = orderNo)

        /**
         * 코스 상세 `?mock=true` 폴백 응답 — 디자인(코스 상세)의 예시 반영. 화면 조합 목
         * ([com.example.backend.bff.adapter.inbound.web.CourseDetailScreenController])과 같은 코스
         * (비 오는 날 성수 감성 카페 코스)로 값을 맞춰 두었다. caption 은 장소명.
         * 실구현 전환 시 호출부와 함께 제거한다.
         */
        val MOCK: CourseDetailResponse =
            CourseDetailResponse(
                course =
                    CourseResponse(
                        id = "1",
                        title = "비 오는 날 성수 감성 카페 코스",
                        coverImageUrl = image("THb4AHDBpbwjQOwLbBj3pgro4xFRpvBdRRZDTcbVmMkg"),
                        theme = "데이트",
                        description =
                            "비가 오면 더 예쁜 성수 카페만 골라 담았어요. 전부 도보로 이어지고, " +
                                "장소마다 제 팁을 남겨뒀으니 참고하세요 🌧️",
                        stats =
                            CourseStatsResponse(
                                placeCount = 4,
                                walkingMinutes = 20,
                                tracingCountLabel = "1.2k",
                            ),
                        authorId = 1L,
                        places =
                            listOf(
                                CoursePlaceResponse(
                                    id = 1L,
                                    placeId = 101L,
                                    orderNo = 1,
                                    caption = "어니언 성수",
                                    walkingMinutesToNext = 6,
                                    images =
                                        listOf(
                                            img("THIxFwvmFDIDNW9rHdqN1wRMZjFTQwfEmgO-O4kBM5nA", 0),
                                            img("Qri_COfUpGil6k79RTh7vRhzDdP08yEcUmXIHnvn7Hfw", 1),
                                        ),
                                ),
                                CoursePlaceResponse(
                                    id = 2L,
                                    placeId = 102L,
                                    orderNo = 2,
                                    caption = "대림창고 갤러리",
                                    walkingMinutesToNext = 3,
                                    images = listOf(img("SYjLV1q0A21vyJJ_N3LlUSp3HwiDDouEZRzcVhJb8KJw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 3L,
                                    placeId = 103L,
                                    orderNo = 3,
                                    caption = "센터커피 성수",
                                    walkingMinutesToNext = 5,
                                    images = listOf(img("TMRMGDnfUqzsxQXY1TOrhMtWZ8-otKbsLPlfnIkvDfUw", 0)),
                                ),
                                CoursePlaceResponse(
                                    id = 4L,
                                    placeId = 104L,
                                    orderNo = 4,
                                    caption = "카페 할아버지공장",
                                    walkingMinutesToNext = null,
                                    images =
                                        listOf(
                                            img("Qr6pSHzsT4DD0ieT5VQ__SVo2ErRODzDyViWmZeXHGlA", 0),
                                            img("R_3CDZ5UcouOOEkvGYQVI2emgnCGRIzRysaKhwNlq-kw", 1),
                                        ),
                                ),
                            ),
                        viewer =
                            CourseViewerResponse(
                                hasSaved = false,
                                hasStartedCourse = false,
                            ),
                    ),
            )
    }
}

data class CourseResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    /** 코스 테마 = 카테고리(단일). 미선택 draft 는 null. */
    val theme: String?,
    val description: String,
    val stats: CourseStatsResponse,
    val authorId: Long,
    val places: List<CoursePlaceResponse>,
    val viewer: CourseViewerResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseResponse =
            CourseResponse(
                id = result.id.toString(),
                title = result.title,
                coverImageUrl = result.coverImageUrl,
                theme = result.theme,
                description = result.description,
                stats = CourseStatsResponse.from(result),
                authorId = result.authorId,
                places = result.places.map(CoursePlaceResponse::from),
                viewer = CourseViewerResponse(result.hasSaved, result.hasStartedCourse),
            )
    }
}

/** 코스 요약 지표. tracingCountLabel 은 표시용 축약("1.2k"). */
data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCountLabel: String,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseStatsResponse =
            CourseStatsResponse(
                placeCount = result.places.size,
                walkingMinutes = result.places.sumOf { it.walkingMinutesToNext ?: 0 },
                tracingCountLabel = abbreviateCount(result.tracingsCnt),
            )
    }
}

/**
 * 코스에 담긴 장소(course_places 행).
 * id 는 course_place 식별자, placeId 는 place 도메인 식별자(별개). orderNo 는 코스 내 장소 순서.
 */
data class CoursePlaceResponse(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    /** 다음 장소까지 도보 이동 시간(분). 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResponse>,
) {
    companion object {
        fun from(place: CoursePlaceResult): CoursePlaceResponse =
            CoursePlaceResponse(
                id = place.id,
                placeId = place.placeId,
                orderNo = place.orderNo,
                caption = place.caption,
                walkingMinutesToNext = place.walkingMinutesToNext,
                images = place.images.map(CoursePlaceImageResponse::from),
            )
    }
}

/** 장소 사진(course_place_images 행). orderNo 는 해당 장소 안에서의 사진 순서. */
data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
) {
    companion object {
        fun from(image: CoursePlaceImageResult): CoursePlaceImageResponse =
            CoursePlaceImageResponse(image.imageUrl, image.orderNo)
    }
}

/** 조회자 관점 상태(저장 여부/코스 시작 여부). */
data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)

/** 카운트를 표시용으로 축약한다(예: 1200 → "1.2k", 1_500_000 → "1.5M"). 1000 미만은 그대로. */
private fun abbreviateCount(count: Int): String {
    fun oneDecimal(
        value: Int,
        unit: Int,
    ): String {
        val whole = value / unit
        val tenth = (value % unit) * 10 / unit
        return if (tenth == 0) "$whole" else "$whole.$tenth"
    }
    return when {
        count < 1_000 -> count.toString()
        count < 1_000_000 -> oneDecimal(count, 1_000) + "k"
        else -> oneDecimal(count, 1_000_000) + "M"
    }
}
