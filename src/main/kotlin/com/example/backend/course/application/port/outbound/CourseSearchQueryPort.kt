package com.example.backend.course.application.port.outbound

import com.example.backend.course.application.port.inbound.CourseSearchSort
import java.time.Instant

/**
 * 아웃바운드 포트 — 공개 코스 **검색**(읽기). 색인([CourseSearchIndexPort])과 짝을 이루는 조회 측으로,
 * OpenSearch 어댑터가 구현한다. 검색은 부가 기능이라 클라이언트가 없으면(로컬·CI) 빈 페이지를 돌려준다(fail-soft).
 */
interface CourseSearchQueryPort {
    fun search(criteria: CourseSearchCriteria): CourseSearchPage
}

/**
 * 검색 조건. 대상은 발행된 PUBLIC 코스로 고정(어댑터가 강제)하고, 여기의 필터는 그 위에 얹는다.
 * [searchAfter] 는 정렬 값(primary, id) 튜플 — 커서에서 복원한 값으로 keyset 페이지네이션(search_after)에 쓴다.
 */
data class CourseSearchCriteria(
    val keyword: String?,
    val area: String?,
    val category: String?,
    val tags: List<String>,
    val sort: CourseSearchSort,
    val searchAfter: List<Any>?,
    val size: Int,
)

/** 검색 히트 한 건 — 코스 요약 + 정렬용 지표. score 는 relevance 정렬일 때만 채워진다(커서 인코딩용). */
data class CourseSearchHit(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val area: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
    val score: Double?,
)

/** 검색 결과 한 페이지 — [size] 로 잘라낸 히트와 다음 페이지 존재 여부(size+1 조회로 판정). */
data class CourseSearchPage(
    val hits: List<CourseSearchHit>,
    val hasNext: Boolean,
)
