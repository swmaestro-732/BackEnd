package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult

data class CourseResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String,
    /** [themes] 의 첫 값 — 다중 카테고리 이전 앱 호환용. 미선택이면 null. */
    val theme: String?,
    /** 코스 테마 = 작성자가 고른 카테고리들. 미선택이면 빈 배열. */
    val themes: List<String>,
    /** 작성자가 단 해시태그. 테마(카테고리)와 별개이며, 순서는 보장하지 않고 없으면 빈 배열. */
    val tags: List<String>,
    val description: String,
    /** 코스 공개 범위(PUBLIC·FOLLOWER·PRIVATE). enum 이름 그대로 직렬화된다. */
    val visibility: CourseVisibility,
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
                theme = result.themes.firstOrNull(),
                themes = result.themes,
                tags = result.tags,
                description = result.description,
                visibility = result.visibility,
                stats = CourseStatsResponse.from(result),
                authorId = result.authorId,
                places = result.places.map(CoursePlaceResponse::from),
                viewer = CourseViewerResponse(result.hasSaved, result.hasLiked, result.hasStartedCourse),
            )
    }
}
