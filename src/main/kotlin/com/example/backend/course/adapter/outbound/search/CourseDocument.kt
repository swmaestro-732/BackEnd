package com.example.backend.course.adapter.outbound.search

/** OpenSearch 색인 문서 — course 인덱스 매핑(opensearch/course.json)과 필드가 일치한다. createdAt 은 epoch millis(date 매핑 호환). */
data class CourseDocument(
    val title: String,
    val description: String?,
    val area: String?,
    val visibility: String,
    val isPublished: Boolean,
    val userId: String,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Long?,
)
