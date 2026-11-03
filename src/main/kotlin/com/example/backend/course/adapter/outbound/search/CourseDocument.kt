package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.outbound.CourseSearchRow
import java.time.Instant

/**
 * OpenSearch 색인 문서 — course 인덱스 매핑(opensearch/course.json)과 필드가 일치한다.
 * 색인(쓰기)과 검색(읽기·역직렬화) 양쪽에서 쓴다. createdAt 은 epoch millis(date 매핑 호환).
 * id 는 검색 정렬 tiebreak(search_after)용으로 문서 본문에도 싣는다(_id 정렬 회피).
 * areaCode 는 코스 법정동코드 10자리, sigunguCode 는 그 앞 5자리 — 추천의 관심 지역 매칭용(가산 매핑이라 구 문서는 null).
 */
data class CourseDocument(
    val id: Long,
    val title: String,
    val description: String?,
    val area: String?,
    val areaCode: String? = null,
    val sigunguCode: String? = null,
    val category: String?,
    val tags: List<String>,
    val coverImageUrl: String?,
    val visibility: String,
    val isPublished: Boolean,
    val userId: String,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Long?,
) {
    /** 검색·추천 응답의 읽기 모델로 변환한다(theme = category 이름). */
    fun toRow(): CourseSearchRow =
        CourseSearchRow(
            id = id,
            authorId = userId.toLong(),
            title = title,
            coverImageUrl = coverImageUrl,
            theme = category,
            area = area,
            likesCnt = likesCnt,
            savesCnt = savesCnt,
            createdAt = Instant.ofEpochMilli(createdAt ?: 0L),
        )
}
