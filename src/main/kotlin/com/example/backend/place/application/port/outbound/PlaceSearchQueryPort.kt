package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceCategory

/**
 * 아웃바운드 포트 — 검색엔진 기반 장소 검색. 정렬된 장소 id 와 전체 매칭 건수만 돌려주고,
 * 본문 조회(hydration)는 호출부가 DB 로 한다(색인 문서에 id·imageUrl 등이 없기도 하다).
 * 검색엔진 미가용·호출 실패 시 null(fail-soft) — 호출부는 null 이면 DB LIKE 폴백으로 전환한다.
 */
interface PlaceSearchQueryPort {
    fun search(criteria: PlaceSearchCriteria): PlaceSearchHits?
}

/** 검색 조건 — 어댑터가 쿼리 DSL 로 번역한다(검색엔진 타입은 여기 드러나지 않는다). */
data class PlaceSearchCriteria(
    /** 필터로 흡수되지 않은 텍스트 토큰들. 비어 있으면 필터-only 브라우즈(최신순 근사). */
    val textTokens: List<String>,
    /** 카테고리 동의어 사전이 흡수한 토큰들의 카테고리(OR). */
    val categories: List<PlaceCategory>,
    /** 지역 사전이 흡수한 토큰들의 법정동코드 prefix(5자리 시군구/10자리 읍면동, OR). */
    val areaCodePrefixes: List<String>,
    /** 오프셋 페이지네이션 시작 위치. */
    val from: Int,
    val size: Int,
)

/** 검색 결과 — 정렬 순서가 보존된 장소 id 목록과 전체 매칭 건수. */
data class PlaceSearchHits(
    val ids: List<Long>,
    val totalCount: Long,
)
