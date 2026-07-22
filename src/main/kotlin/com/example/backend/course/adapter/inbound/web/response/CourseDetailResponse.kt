package com.example.backend.course.adapter.inbound.web.response

/**
 * 웹 응답 DTO — 코스 상세. 프론트 계약대로 최상위를 { "course": {...} } 로 감싼다
 * (공통 ApiResponse.data 안에 들어간다). id 는 외부 계약상 문자열로 다룬다.
 */
data class CourseDetailResponse(
    val course: CourseResponse,
)

data class CourseResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    val description: String,
    val stats: CourseStatsResponse,
    val authorId: Long,
    val places: List<CoursePlaceResponse>,
    val viewer: CourseViewerResponse,
)

/** 코스 요약 지표. tracingCountLabel 은 표시용 축약("1.2k"). */
data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCountLabel: String,
)

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
)

/** 장소 사진(course_place_images 행). orderNo 는 해당 장소 안에서의 사진 순서. */
data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
)

/** 조회자 관점 상태(저장 여부/코스 시작 여부). */
data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)
