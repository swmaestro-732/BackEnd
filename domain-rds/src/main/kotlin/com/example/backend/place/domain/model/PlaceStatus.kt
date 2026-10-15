package com.example.backend.place.domain.model

/** 장소 상태. enum 이름이 DB 저장 계약 — 상수 이름 변경 금지(값 추가는 무방). */
enum class PlaceStatus {
    ACTIVE,
    HIDDEN,
    SUSPENDED,
    DELETED,
}
