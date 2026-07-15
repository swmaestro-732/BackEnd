package com.example.backend.place.domain.model

import com.example.backend.common.persistence.CodedEnum

/** 장소 영업 상태. code 는 DB 저장 계약 — 변경 금지. DB 기본값은 UNKNOWN(3). */
enum class PlaceBusinessStatus(
    override val code: Short,
) : CodedEnum {
    OPEN(0),
    TEMPORARILY_CLOSED(1),
    PERMANENTLY_CLOSED(2),
    UNKNOWN(3),
}
