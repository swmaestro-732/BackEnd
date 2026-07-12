package com.example.backend.course.application.dto

/**
 * 유스케이스 출력 — 코스 상세. 도메인 모델을 밖으로 노출하지 않기 위한 애플리케이션 경계 타입.
 * 프론트 상세 화면 계약에 맞춘 뷰 지향 구조다. id 는 외부 계약상 문자열로 다룬다.
 */
data class CourseDetailResult(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    val themes: List<String>,
    val description: String,
    val stats: CourseStatsResult,
    val authorId: Long,
    val places: List<CoursePlaceResult>,
    val viewer: CourseViewerResult,
)

/**
 * 코스에 담긴 장소(course_places 행).
 * id 는 course_place 식별자, placeId 는 place 도메인 식별자(별개). orderNo 는 코스 내 장소 순서.
 */
data class CoursePlaceResult(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val subcaption: String?,
    /** 다음 장소까지 도보 이동 시간(분). 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResult>,
)

/** 장소 사진(course_place_images 행). orderNo 는 해당 장소 안에서의 사진 순서. */
data class CoursePlaceImageResult(
    val imageUrl: String,
    val orderNo: Int,
)

/** 코스 요약 지표. tracingCountLabel 은 표시용 축약("1.2k"). */
data class CourseStatsResult(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCountLabel: String,
)

/** 조회자 관점 상태(저장 여부/코스 시작 여부). */
data class CourseViewerResult(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)
