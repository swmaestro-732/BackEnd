package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 반환 DTO — 다른 도메인(course)에 노출하는 장소 요약.
 * 장소 도메인 모델([com.example.backend.place.domain.model.Place])을 경계 밖으로 새지 않게 하려고 둔다.
 *
 * - [id] 장소 식별자.
 * - [category] 장소 카테고리 코드(도메인 enum 이름). 코스 카테고리 도출 입력으로만 쓰인다.
 */
data class PlaceSummary(
    val id: Long,
    val category: String,
)
