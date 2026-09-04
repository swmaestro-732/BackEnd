package com.example.backend.course.adapter.outbound.search

/**
 * OpenSearch 색인 문서 — course 인덱스 매핑(opensearch/course.json)과 필드가 일치한다.
 * 색인(쓰기)과 검색(읽기·역직렬화) 양쪽에서 쓴다. createdAt 은 epoch millis(date 매핑 호환).
 * id 는 검색 정렬 tiebreak(search_after)용으로 문서 본문에도 싣는다(_id 정렬 회피).
 */
data class CourseDocument(
    val id: Long,
    val title: String,
    val description: String?,
    val area: String?,
    val category: String?,
    val tags: List<String>,
    val coverImageUrl: String?,
    val visibility: String,
    val isPublished: Boolean,
    val userId: String,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Long?,
)
