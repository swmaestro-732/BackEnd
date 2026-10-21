package com.example.backend.chat.domain.model

/** 방-지역 매핑 종류. AREA_PREFIX 는 시군구 코드, H3_CELL 은 H3 셀 id. enum 이름이 DB 저장 계약이다. */
enum class ChatRegionType {
    AREA_PREFIX,
    H3_CELL,
}
