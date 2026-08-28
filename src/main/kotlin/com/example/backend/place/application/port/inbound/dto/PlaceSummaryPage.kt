package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 반환 DTO — 장소 검색 커서 페이지.
 *
 * - [items] 이번 페이지 장소 요약들(검색 경로의 정렬 순서 보존).
 * - [totalCount] 검색어에 매칭되는 전체 개수("장소 N곳"). 페이지 크기가 아니라 전체 집계다.
 * - [hasNext] 다음 페이지 존재 여부.
 * - [nextCursor] 다음 페이지 불투명 커서 — 경로(검색엔진 오프셋/DB keyset)별 형식이 달라 웹 계층이
 *   도출할 수 없으므로 서비스가 발급한다. [hasNext] 가 false 면 null.
 */
data class PlaceSummaryPage(
    val items: List<PlaceSummary>,
    val totalCount: Int,
    val hasNext: Boolean,
    val nextCursor: String? = null,
)
