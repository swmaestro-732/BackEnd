package com.example.backend.course.application.port.inbound.dto

/** 작성자의 발행·활성·미삭제 코스 개수를 공개범위별로 집계한 결과. */
data class CourseVisibilityCounts(
    val publicCount: Int,
    val followerCount: Int,
    val privateCount: Int,
)
