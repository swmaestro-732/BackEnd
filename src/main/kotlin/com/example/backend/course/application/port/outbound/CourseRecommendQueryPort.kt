package com.example.backend.course.application.port.outbound

import java.time.Instant

/**
 * 아웃바운드 포트 — 추천 코스 랭킹 조회(검색 엔진). 발행 PUBLIC 코스를 점수순으로 돌려준다.
 * 엔진이 없거나(로컬·CI) 호출이 실패하면 null 을 돌려주고(fail-soft) 호출자가 DB 폴백을 결정한다.
 */
interface CourseRecommendQueryPort {
    fun recommend(criteria: CourseRecommendCriteria): CourseRecommendResultPage?
}

/**
 * 추천 조건. 관심 지역은 호출자가 시군구(앞 5자리)·읍면동(10자리, 동 선택분만)으로 갈라 준다.
 * [origin] 은 최신성 감쇠 기준 시각 — 페이지를 넘겨도 같은 값을 써야 점수가 흔들리지 않는다.
 * [after] 는 직전 페이지 마지막 히트의 (점수, id) — search_after 용. 첫 페이지는 null.
 */
data class CourseRecommendCriteria(
    val likeThemes: List<String>,
    val sigunguCodes: List<String>,
    val dongCodes: List<String>,
    val category: String?,
    val origin: Instant,
    val after: CourseRecommendAfter?,
    val size: Int,
)

data class CourseRecommendAfter(
    val score: Double,
    val id: Long,
)

/** 추천 한 건 — 요약 행과 엔진 점수(다음 커서용). */
data class CourseRecommendHit(
    val row: CourseSearchRow,
    val score: Double,
)

data class CourseRecommendResultPage(
    val hits: List<CourseRecommendHit>,
    val hasNext: Boolean,
)
