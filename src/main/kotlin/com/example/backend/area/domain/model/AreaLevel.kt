package com.example.backend.area.domain.model

/**
 * 행정구역 레벨. 법정동코드 prefix 길이로 결정된다: 2자리=시도, 5자리=시군구, 10자리=읍면동.
 * 저장 단위는 읍면동(DONG)뿐이며, SIDO·SIGUNGU 는 읍면동 데이터에서 파생되는 조회 단위다.
 */
enum class AreaLevel {
    SIDO,
    SIGUNGU,
    DONG,
}
