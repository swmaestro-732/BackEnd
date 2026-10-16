package com.example.backend.course.adapter.inbound.web.response

/** 코스 좋아요/취소 응답 — 토글 후 상태(liked)와 코스 좋아요 수(likesCnt). */
data class CourseLikeResponse(
    val courseId: Long,
    val liked: Boolean,
    val likesCnt: Int,
)
