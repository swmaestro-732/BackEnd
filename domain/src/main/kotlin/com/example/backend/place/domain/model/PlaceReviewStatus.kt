package com.example.backend.place.domain.model

/** 장소 리뷰 상태. enum 이름이 DB 저장 계약 — 상수 이름 변경 금지(값 추가는 무방). */
enum class PlaceReviewStatus {
    PUBLISHED,
    HIDDEN,
    DELETED,
}
