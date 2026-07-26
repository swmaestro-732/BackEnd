package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.CourseVisibility

/**
 * 코스 생성 명령(애플리케이션 경계 타입). 웹 요청(CreateCourseRequest)에서 매핑된다.
 * coverImageUrl 은 요청의 thumbnailUrl(코스 커버). category 는 담은 장소들의 카테고리로 서비스가 도출한다.
 */
data class CreateCourseCommand(
    val userId: Long,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val forkedFromId: Long?,
    val places: List<CreateCoursePlaceCommand>,
)

data class CreateCoursePlaceCommand(
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val imageUrls: List<String>,
)
