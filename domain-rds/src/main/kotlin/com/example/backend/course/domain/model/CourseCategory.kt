package com.example.backend.course.domain.model

/**
 * 코스 카테고리. DB `courses.category` 에 이름 문자열로 저장된다(V3 enum 저장 컨벤션 — enumerationByName).
 * 카테고리 미선택 임시저장(draft) 코스가 있어 nullable.
 */
enum class CourseCategory {
    DATE,
    HEALING,
    FOOD,
    CAFETOUR,
    CULTURE,
    NATURE,
    NIGHTVIEW,
    SHOPPING,
    TRADITION,
    ACTIVITY,
    FAMILY,
    SOLO,
    ;

    companion object {
        /**
         * 코스에 담긴 장소들의 카테고리(PlaceCategory 이름, orderNo 오름차순)로 코스 카테고리를 도출한다.
         * 각 장소 카테고리를 코스 카테고리로 매핑한 뒤 최빈값을 고른다(동률이면 orderNo 빠른 장소 우선).
         * 매핑되는 장소가 없으면 null(미선택 draft).
         *
         * PlaceCategory 는 다른 도메인(place) 타입이라 직접 참조하지 않고 이름 문자열로 받는다(크로스 도메인 격리).
         */
        fun fromPlaceCategoryNames(orderedPlaceCategoryNames: List<String>): CourseCategory? {
            val mapped = orderedPlaceCategoryNames.mapNotNull(::mapPlaceCategory)
            if (mapped.isEmpty()) return null
            val counts = mapped.groupingBy { it }.eachCount()
            val maxCount = counts.values.max()
            // orderNo 오름차순으로 순회하며 최다 빈도 카테고리 중 먼저 나온 것 → 동률은 앞선 장소 우선.
            return mapped.first { counts.getValue(it) == maxCount }
        }

        private fun mapPlaceCategory(placeCategoryName: String): CourseCategory? =
            when (placeCategoryName) {
                "CAFE" -> CAFETOUR
                "RESTAURANT" -> FOOD
                "BAR" -> NIGHTVIEW
                "SHOPPING" -> SHOPPING
                "CULTURE", "LANDMARK" -> CULTURE
                "NATURE" -> NATURE
                "EXPERIENCE", "ENTERTAINMENT" -> ACTIVITY
                "WELLNESS" -> HEALING
                else -> null
            }
    }
}
