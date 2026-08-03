package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 반환 DTO — 장소 검색 커서 페이지.
 *
 * - [items] 이번 페이지 장소 요약들(id 오름차순).
 * - [totalCount] 검색어에 매칭되는 전체 개수("장소 N곳"). 페이지 크기가 아니라 전체 집계다.
 * - [hasNext] 다음 페이지 존재 여부. 커서(다음 요청 기준값)는 [items] 마지막 장소의 id 로 웹 계층이 도출한다.
 */
data class PlaceSummaryPage(
    val items: List<PlaceSummary>,
    val totalCount: Int,
    val hasNext: Boolean,
)
