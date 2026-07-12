package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.dto.CourseDetailResult
import com.example.backend.course.application.dto.CoursePlaceImageResult
import com.example.backend.course.application.dto.CoursePlaceResult
import com.example.backend.course.application.dto.CourseStatsResult
import com.example.backend.course.application.dto.CourseViewerResult

/**
 * 웹 응답 DTO. 유스케이스 결과([CourseDetailResult])를 직렬화 형태로 변환한다.
 * 프론트 계약대로 최상위를 { "course": {...} } 로 감싼다(공통 ApiResponse.data 안에 들어간다).
 */
data class CourseDetailResponse(
    val course: CourseResponse,
) {
    companion object {
        fun from(result: CourseDetailResult): CourseDetailResponse =
            CourseDetailResponse(course = CourseResponse.from(result))
    }
}

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
) {
    companion object {
        fun from(result: CourseDetailResult): CourseResponse =
            CourseResponse(
                id = result.id,
                title = result.title,
                coverImageUrl = result.coverImageUrl,
                themes = result.themes,
                description = result.description,
                stats = CourseStatsResponse.from(result.stats),
                authorId = result.authorId,
                places = result.places.map(CoursePlaceResponse::from),
                viewer = CourseViewerResponse.from(result.viewer),
            )
    }
}

data class CourseStatsResponse(
    val placeCount: Int,
    val walkingMinutes: Int,
    val tracingCountLabel: String,
) {
    companion object {
        fun from(result: CourseStatsResult): CourseStatsResponse =
            CourseStatsResponse(
                placeCount = result.placeCount,
                walkingMinutes = result.walkingMinutes,
                tracingCountLabel = result.tracingCountLabel,
            )
    }
}

data class CoursePlaceResponse(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val subcaption: String?,
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResponse>,
) {
    companion object {
        fun from(result: CoursePlaceResult): CoursePlaceResponse =
            CoursePlaceResponse(
                id = result.id,
                placeId = result.placeId,
                orderNo = result.orderNo,
                caption = result.caption,
                subcaption = result.subcaption,
                walkingMinutesToNext = result.walkingMinutesToNext,
                images = result.images.map(CoursePlaceImageResponse::from),
            )
    }
}

data class CoursePlaceImageResponse(
    val imageUrl: String,
    val orderNo: Int,
) {
    companion object {
        fun from(result: CoursePlaceImageResult): CoursePlaceImageResponse =
            CoursePlaceImageResponse(imageUrl = result.imageUrl, orderNo = result.orderNo)
    }
}

data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
) {
    companion object {
        fun from(result: CourseViewerResult): CourseViewerResponse =
            CourseViewerResponse(hasSaved = result.hasSaved, hasStartedCourse = result.hasStartedCourse)
    }
}
