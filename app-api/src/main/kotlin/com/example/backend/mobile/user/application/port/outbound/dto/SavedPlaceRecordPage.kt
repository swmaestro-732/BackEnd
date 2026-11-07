package com.example.backend.mobile.user.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 저장 레코드 한 페이지 + 배지 카운트. user 도메인 응답
 * ([com.example.backend.user.application.port.inbound.dto.SavedPlacesResult])을 BFF 안으로 복사한 격리 DTO다.
 *
 * 카운트는 조회 필터(visited·category)와 무관한 전체 기준이다(탭·칩 배지가 어느 탭에서도 같은 값).
 */
data class SavedPlaceRecordPage(
    val totalCount: Long,
    val unvisitedCount: Long,
    val visitedCount: Long,
    val categoryCounts: List<SavedPlaceCategoryCount>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val records: List<SavedPlaceRecord>,
)

/** 카테고리 칩 배지 — category 는 저장 카테고리 이름 문자열(예: CAFE). */
data class SavedPlaceCategoryCount(
    val category: String,
    val count: Long,
)

/** 저장 레코드 하나 — 장소 정보는 붙지 않은 저장 사실만 담는다(장소는 place 포트로 따로 조회). */
data class SavedPlaceRecord(
    // 저장 레코드 id (장소 id 아님)
    val id: Long,
    val placeId: Long,
    // 저장 시 복사한 카테고리 스냅샷 이름(미분류면 null)
    val category: String?,
    val visited: Boolean,
    val savedAt: Instant,
)
