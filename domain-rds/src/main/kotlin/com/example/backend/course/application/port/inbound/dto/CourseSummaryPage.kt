package com.example.backend.course.application.port.inbound.dto

/** 코스 요약 한 페이지(작성자 목록·공개 피드 공용). [items]는 조회 정렬 순서이며 [hasNext]는 다음 페이지 존재 여부다. */
data class CourseSummaryPage(
    val items: List<CourseSummary>,
    val hasNext: Boolean,
)
