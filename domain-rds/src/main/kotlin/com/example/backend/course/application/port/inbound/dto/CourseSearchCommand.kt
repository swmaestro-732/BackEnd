package com.example.backend.course.application.port.inbound.dto

/** 검색 정렬 기준. relevance=_score, latest=createdAt, popular=savesCnt — 각각 코스 id 로 안정 tiebreak 한다. */
enum class CourseSearchSort {
    RELEVANCE,
    LATEST,
    POPULAR,
}

/** 검색 요청 — 컨트롤러가 쿼리 파라미터를 그대로 옮겨 담는다. 빈 문자열 정규화는 서비스가 한다. */
data class CourseSearchCommand(
    val keyword: String?,
    val area: String?,
    val category: String?,
    val tags: List<String>,
    val sort: CourseSearchSort,
    val cursor: String?,
    val size: Int,
)
