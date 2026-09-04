package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.outbound.CourseSearchHit

/**
 * 인바운드 포트 — 공개 코스 검색(`GET /api/v1/courses/search`). 키워드·필터·정렬로 발행 PUBLIC 코스를 찾는다.
 * 커서(문자열) 디코딩/인코딩과 검색 포트 호출을 서비스가 담당하고, 컨트롤러는 Request→포트→Response 매핑만 한다.
 */
interface CourseSearchUseCase {
    fun search(command: CourseSearchCommand): CourseSearchResult
}

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

/** 검색 결과 — 히트 목록과 다음 커서. 재료는 아웃바운드 검색 DTO([CourseSearchHit])를 그대로 쓴다(HomeFeed 와 동일 관례). */
data class CourseSearchResult(
    val courses: List<CourseSearchHit>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
