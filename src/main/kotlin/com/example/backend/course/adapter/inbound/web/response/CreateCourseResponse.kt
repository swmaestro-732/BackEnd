package com.example.backend.course.adapter.inbound.web.response

/** 코스 생성 응답 — 생성된 코스 식별자만 내려준다("코스 완성" 화면은 코스 상세 API 재조회로 구성). */
data class CreateCourseResponse(
    val courseId: Long,
) {
    companion object {
        /** 모킹 고정 응답 — 코스 상세 목 데이터(courseId=1)와 이어지도록 항상 1을 반환한다. */
        fun mock(): CreateCourseResponse = CreateCourseResponse(courseId = 1L)
    }
}
