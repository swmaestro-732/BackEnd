package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.dto.CourseSummary
import com.example.backend.course.application.port.outbound.CoursePlaceStats
import com.example.backend.course.application.port.outbound.CourseSearchRow
import com.example.backend.course.application.port.outbound.CourseSummaryRow

/** 영속 요약 행 → 코스 요약. [stats] 가 없으면 장소 수·도보 시간은 채우지 않는다(null). */
fun CourseSummaryRow.toCourseSummary(stats: CoursePlaceStats? = null): CourseSummary =
    CourseSummary(
        id = id,
        authorId = userId,
        title = title,
        coverImageUrl = coverImageUrl,
        themes = categories.map { it.name },
        area = area,
        likesCnt = likesCnt,
        savesCnt = savesCnt,
        createdAt = createdAt,
        placeCount = stats?.placeCount,
        walkingMinutes = stats?.walkingMinutes,
    )

/** 검색 엔진 행 → 코스 요약. [stats] 가 없으면 장소 수·도보 시간은 채우지 않는다(null). */
fun CourseSearchRow.toCourseSummary(stats: CoursePlaceStats? = null): CourseSummary =
    CourseSummary(
        id = id,
        authorId = authorId,
        title = title,
        coverImageUrl = coverImageUrl,
        themes = themes,
        area = area,
        likesCnt = likesCnt,
        savesCnt = savesCnt,
        createdAt = createdAt,
        placeCount = stats?.placeCount,
        walkingMinutes = stats?.walkingMinutes,
    )
