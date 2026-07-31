package com.example.backend.course.adapter.inbound.web.response

/** 조회자 관점 상태(저장 여부/코스 시작 여부). */
data class CourseViewerResponse(
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)
