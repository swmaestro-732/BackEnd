package com.example.backend.user.domain.model

/**
 * 저장 장소 카테고리 — 장소 저장 시 place 도메인의 카테고리를 복사해 두는 스냅샷 값
 * (MSA 대비 비정규화: 저장함 카테고리 필터를 user 도메인 테이블만으로 처리하기 위함).
 *
 * place 도메인의 `PlaceCategory` 와 같은 상수 집합이지만, 크로스 도메인 격리 규칙
 * (도메인은 다른 도메인의 inbound 포트만 참조)에 따라 사본으로 둔다 — PlaceCategory 변경 시 함께 갱신한다.
 * DB `saved_places.category` 에 이름 문자열로 저장된다(V3 enum 저장 컨벤션 — enumerationByName).
 */
enum class SavedPlaceCategory {
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
