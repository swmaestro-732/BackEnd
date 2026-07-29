package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.CourseVisibility

/**
 * 코스 상세 조회 결과(애플리케이션 계층 DTO). 웹 응답 매퍼가 표시 로직을 입힌다 —
 * tracingsCnt(원시값) → 축약 라벨, walkingMinutesToNext 합계 → stats.walkingMinutes 등은
 * 여기가 아니라 web 계층에서 계산한다.
 */
data class CourseDetailResult(
    val id: Long,
    val title: String,
    val coverImageUrl: String,
    /** 코스 테마 = 카테고리(단일). 미선택 draft 는 null. */
    val theme: String?,
    val description: String,
    /** 코스 공개 범위(PUBLIC·FOLLOWER·PRIVATE). */
    val visibility: CourseVisibility,
    val authorId: Long,
    val tracingsCnt: Int,
    val places: List<CoursePlaceResult>,
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)

data class CoursePlaceResult(
    val id: Long,
    val placeId: Long,
    val orderNo: Int,
    val caption: String?,
    /** 다음 장소까지 도보 이동 시간(분). 마지막 장소면 null. */
    val walkingMinutesToNext: Int?,
    val images: List<CoursePlaceImageResult>,
)

data class CoursePlaceImageResult(
    val imageUrl: String,
    val orderNo: Int,
)
