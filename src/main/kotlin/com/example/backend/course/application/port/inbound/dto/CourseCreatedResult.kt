package com.example.backend.course.application.port.inbound.dto

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.domain.model.Course
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** 저장 결과를 도메인 enum·모델 없이 공개해 다른 도메인도 생성 유스케이스를 사용할 수 있게 한다. */
data class CourseCreatedResult(
    val id: Long?,
    val userId: Long,
    val status: String,
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val category: String?,
    val area: String?,
    val areaCode: String?,
    val visitDate: LocalDate?,
    val visibility: CourseVisibility,
    val isPublished: Boolean,
    val likesCnt: Int,
    val commentsCnt: Int,
    val savesCnt: Int,
    val tracingsCnt: Int,
    val duplicatedFromId: Long?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
    val tags: List<String>,
    val places: List<CreatedCoursePlaceResult>,
)

data class CreatedCoursePlaceResult(
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    val imageUrls: List<String>,
    val walkingMinutes: Int?,
)

fun Course.toCreatedResult() =
    CourseCreatedResult(
        id = id,
        userId = userId,
        status = status.name,
        title = title,
        description = description,
        coverImageUrl = coverImageUrl,
        category = category?.name,
        area = area,
        areaCode = areaCode,
        visitDate = visitDate,
        visibility = visibility,
        isPublished = isPublished,
        likesCnt = likesCnt,
        commentsCnt = commentsCnt,
        savesCnt = savesCnt,
        tracingsCnt = tracingsCnt,
        duplicatedFromId = duplicatedFromId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        tags = tags,
        places =
            places.map {
                CreatedCoursePlaceResult(
                    it.placeId,
                    it.orderNo,
                    it.caption,
                    it.imageUrls,
                    it.walkingMinutes,
                )
            },
    )
