package com.example.backend.course.domain.model

/**
 * 코스 카테고리. 코스마다 여러 개를 가지며, DB `course_categories.category` 에 이름 문자열로 저장된다
 * (V3 enum 저장 컨벤션 — enumerationByName). [COMPANIONS] 는 작성자가 고르고, 나머지는 담은 장소로 정한다.
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
        /** 작성자가 요청으로 고르는 카테고리(데이트·가족 나들이·혼놀). */
        val COMPANIONS: Set<CourseCategory> = setOf(DATE, FAMILY, SOLO)

        /**
         * 작성자가 고른 [companions] 와 담은 장소들의 카테고리(PlaceCategory 이름)로 코스 카테고리를 정한다.
         * 장소 카테고리에 대응하는 코스 카테고리를 모두 붙이고, 그 결과가 1개 이하면 음식점·카페 중 많은 쪽(동률이면 둘 다)을 더한다.
         *
         * PlaceCategory 는 다른 도메인(place) 타입이라 직접 참조하지 않고 이름 문자열로 받는다(크로스 도메인 격리).
         */
        fun resolve(
            companions: Collection<CourseCategory>,
            placeCategoryNames: List<String>,
        ): Set<CourseCategory> {
            val categories = companions.toSet() + placeCategoryNames.flatMap(::mapPlaceCategory)
            if (categories.size > 1) return categories
            val restaurants = placeCategoryNames.count { it == "RESTAURANT" }
            val cafes = placeCategoryNames.count { it == "CAFE" }
            return categories +
                listOfNotNull(
                    FOOD.takeIf { restaurants > 0 && restaurants >= cafes },
                    CAFETOUR.takeIf { cafes > 0 && cafes >= restaurants },
                )
        }

        private fun mapPlaceCategory(placeCategoryName: String): List<CourseCategory> =
            when (placeCategoryName) {
                "CULTURE" -> listOf(CULTURE)
                "NATURE" -> listOf(NATURE, HEALING)
                "LANDMARK" -> listOf(TRADITION)
                "BAR" -> listOf(NIGHTVIEW)
                "SHOPPING" -> listOf(SHOPPING)
                "EXPERIENCE", "ENTERTAINMENT" -> listOf(ACTIVITY)
                "WELLNESS" -> listOf(HEALING)
                else -> emptyList()
            }
    }
}
