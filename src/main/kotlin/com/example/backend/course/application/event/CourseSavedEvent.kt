package com.example.backend.course.application.event

import com.example.backend.course.domain.model.Course

/** 도메인 이벤트 — 검색 인덱스 동기화 등 커밋 후 후처리에 쓴다. */
data class CourseSavedEvent(
    val course: Course,
)
