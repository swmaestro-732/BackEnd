package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.inbound.dto.CourseSearchHit

/**
 * 아웃바운드 포트 — 공개 코스 **검색**(읽기). 색인([CourseSearchIndexPort])과 짝을 이루는 조회 측으로,
 * OpenSearch 어댑터가 구현한다. 검색은 부가 기능이라 클라이언트가 없으면(로컬·CI) 빈 페이지를 돌려준다(fail-soft).
 *
 * 커서는 **불투명 문자열**로만 오간다 — 정렬 값(search_after) 인코딩/디코딩과 다음 커서 생성은 검색 엔진 세부라
 * 어댑터가 소유한다(애플리케이션은 커서 내용을 모른다).
 */
interface CourseSearchQueryPort {
    fun search(criteria: CourseSearchCriteria): CourseSearchPage
}

/**
 * 검색 조건. 대상은 발행된 PUBLIC 코스로 고정(어댑터가 강제)하고, 여기의 필터는 그 위에 얹는다.
 * [cursor] 는 이전 페이지가 준 불투명 커서(첫 페이지는 null) — 어댑터가 해석해 keyset 페이지네이션에 쓴다.
 */
data class CourseSearchCriteria(
    val keyword: String?,
    val area: String?,
    val category: String?,
    val tags: List<String>,
    val sort: CourseSearchSort,
    val cursor: String?,
    val size: Int,
)

/**
 * 검색 결과 한 페이지 — [size] 로 잘라낸 히트, 다음 페이지 존재 여부(size+1 조회로 판정), 다음 페이지용 불투명 [nextCursor].
 * 다음 페이지가 없으면 [nextCursor] 는 null.
 */
data class CourseSearchPage(
    val hits: List<CourseSearchHit>,
    val hasNext: Boolean,
    val nextCursor: String?,
)
