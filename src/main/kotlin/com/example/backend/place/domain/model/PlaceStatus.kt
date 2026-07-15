package com.example.backend.place.domain.model

import com.example.backend.common.persistence.CodedEnum

/** 장소 상태. code 는 DB 저장 계약 — 변경 금지. */
enum class PlaceStatus(
    override val code: Short,
) : CodedEnum {
    ACTIVE(0),
    HIDDEN(1),
    SUSPENDED(2),
    DELETED(3),
}
