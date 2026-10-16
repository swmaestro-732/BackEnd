package com.example.backend.user.application.port.inbound.dto

/** 특정 사용자의 코스 관점 상태 — 코스 저장 여부. 코스 상세 화면의 조회자 상태(viewer)에 쓴다. */
data class CourseViewerState(
    val courseId: Long,
    val hasSaved: Boolean,
)
