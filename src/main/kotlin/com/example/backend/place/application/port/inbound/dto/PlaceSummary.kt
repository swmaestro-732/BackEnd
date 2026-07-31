package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 반환 DTO — 다른 도메인(course)에 노출하는 장소 요약.
 * 장소 도메인 모델([com.example.backend.place.domain.model.Place])을 경계 밖으로 새지 않게 하려고 둔다.
 *
 * - [id] 장소 식별자.
 * - [name] 장소명(화면 표시용).
 * - [category] 장소 카테고리 코드(도메인 enum 이름). 코스 카테고리 도출 입력으로도 쓰인다.
 * - [address] 장소 주소(화면 표시용).
 * - [imageUrl] 장소 대표 이미지(places.image_url, 없을 수 있음).
 * - [latitude]·[longitude] 장소 좌표(지도 핀 표시용). 코스 상세 화면에서 장소별 위치를 찍는 데 쓴다.
 */
data class PlaceSummary(
    val id: Long,
    val name: String,
    val category: String,
    val address: String,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
)
