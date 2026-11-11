package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.outbound.CourseSearchRow
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

/**
 * OpenSearch 색인 문서 — course 인덱스 매핑(opensearch/course.json)과 필드가 일치한다.
 * 색인(쓰기)과 검색(읽기·역직렬화) 양쪽에서 쓴다. createdAt 은 epoch millis(date 매핑 호환).
 * id 는 검색 정렬 tiebreak(search_after)용으로 문서 본문에도 싣는다(_id 정렬 회피).
 * category 는 keyword 배열(다중 카테고리)이다 — 재색인 전 문서는 단일 문자열·null 이라 단일값도 배열로 읽는다.
 * areaCode 는 코스 법정동코드 10자리, sigunguCode 는 그 앞 5자리 — 추천의 관심 지역 매칭용(가산 매핑이라 구 문서는 null).
 * placeIds 는 코스에 담긴 장소 id — "이 장소가 들어간 코스" 필터용(가산 매핑이라 재색인 전 문서는 빈 배열).
 */
data class CourseDocument(
    val id: Long,
    val title: String,
    val description: String?,
    val area: String?,
    val areaCode: String? = null,
    val sigunguCode: String? = null,
    val placeIds: List<String> = emptyList(),
    @field:JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val category: List<String>?,
    val tags: List<String>,
    val coverImageUrl: String?,
    val visibility: String,
    val isPublished: Boolean,
    val userId: String,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Long?,
) {
    /** 검색·추천 응답의 읽기 모델로 변환한다(themes = category 이름 목록). */
    fun toRow(): CourseSearchRow =
        CourseSearchRow(
            id = id,
            authorId = userId.toLong(),
            title = title,
            coverImageUrl = coverImageUrl,
            themes = category.orEmpty(),
            area = area,
            likesCnt = likesCnt,
            savesCnt = savesCnt,
            createdAt = Instant.ofEpochMilli(createdAt ?: 0L),
        )
}
