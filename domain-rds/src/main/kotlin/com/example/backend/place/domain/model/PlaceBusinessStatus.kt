package com.example.backend.place.domain.model

/** 장소 영업 상태. enum 이름이 DB 저장 계약 — 상수 이름 변경 금지(값 추가는 무방). DB 기본값은 UNKNOWN. */
enum class PlaceBusinessStatus {
    OPEN,
    TEMPORARILY_CLOSED,
    PERMANENTLY_CLOSED,
    UNKNOWN,
}
