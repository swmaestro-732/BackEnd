package com.example.backend.place.domain.model

/**
 * 장소 카테고리. DB `places.category` 에 이름 문자열로 저장된다(V3 enum 저장 컨벤션 — enumerationByName).
 */
enum class PlaceCategory {
    CAFE,
    RESTAURANT,
    BAR,
    SHOPPING,
    CULTURE,
    EXPERIENCE,
    NATURE,
    LANDMARK,
    ENTERTAINMENT,
    WELLNESS,
}
