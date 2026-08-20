package com.example.backend.place.application.port.inbound.dto

/**
 * 인바운드 포트 입력 DTO — 장소 리뷰 작성.
 *
 * - [placeId] 리뷰를 남길 장소. 존재하지 않으면 404.
 * - [userId] 작성자(JWT subject).
 * - [rating] 별점 1~5.
 * - [content] "한마디 남기기". 별점만 남길 수 있어 선택 값이고, 공백만 있으면 없는 것으로 본다.
 * - [photoUrls] 업로드 presign 으로 올린 이미지 URL 목록. 순서가 곧 노출 순서다.
 * - [tagCodes] 선택한 태그의 코드(`.ai/taxonomy.md` 키워드 — coffee, view …).
 *   웹 계약이 코드 문자열이라 포트도 문자열로 받고, 도메인 enum([com.example.backend.place.domain.model.PlaceReviewTag])
 *   변환·검증은 서비스가 한다(모르는 코드는 400).
 */
data class CreatePlaceReviewCommand(
    val placeId: Long,
    val userId: Long,
    val rating: Int,
    val content: String?,
    val photoUrls: List<String>,
    val tagCodes: List<String>,
)
