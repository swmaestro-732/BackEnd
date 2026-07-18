package com.example.backend.course.adapter.inbound.web.response

/** 코스 생성 응답 — 생성된 코스 식별자만 내려준다("코스 완성" 화면은 코스 상세 API 재조회로 구성). */
data class CreateCourseResponse(
    val courseId: Long,
)
