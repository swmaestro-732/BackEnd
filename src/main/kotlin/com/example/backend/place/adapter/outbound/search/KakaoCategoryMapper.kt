package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.domain.model.PlaceCategory

/**
 * 카카오 분류 → 내부 [PlaceCategory] 매핑.
 * `category_name`(대 > 중 > 소분류)을 단계 단위 접두로 매칭하고(가장 긴 접두 우선), 매칭이 없으면 `category_group_code` 로,
 * 그것도 없으면 코스와 무관한 장소로 보고 [PlaceCategory.WELLNESS] 로 둔다.
 */
object KakaoCategoryMapper {
    private val RULES: List<Pair<List<String>, PlaceCategory>> =
        listOf(
            "음식점 > 카페" to PlaceCategory.CAFE,
            "음식점 > 간식 > 제과,베이커리" to PlaceCategory.CAFE,
            "음식점 > 술집" to PlaceCategory.BAR,
            "음식점" to PlaceCategory.RESTAURANT,
            "가정,생활 > 백화점" to PlaceCategory.SHOPPING,
            "가정,생활 > 복합쇼핑몰" to PlaceCategory.SHOPPING,
            "가정,생활 > 상설할인매장" to PlaceCategory.SHOPPING,
            "가정,생활 > 패션" to PlaceCategory.SHOPPING,
            "가정,생활 > 시장" to PlaceCategory.SHOPPING,
            "가정,생활 > 생활용품점" to PlaceCategory.SHOPPING,
            "가정,생활 > 편의점" to PlaceCategory.SHOPPING,
            "가정,생활 > 슈퍼마켓" to PlaceCategory.SHOPPING,
            "이슈 > 이벤트 > 팝업스토어" to PlaceCategory.SHOPPING,
            "여행 > 기념품판매" to PlaceCategory.SHOPPING,
            "문화,예술 > 문화시설" to PlaceCategory.CULTURE,
            "문화,예술 > 미술,공예 > 화랑" to PlaceCategory.CULTURE,
            "문화,예술 > 도서" to PlaceCategory.CULTURE,
            "이슈 > 이벤트 > 공연" to PlaceCategory.CULTURE,
            "이슈 > 이벤트 > 페스티벌" to PlaceCategory.CULTURE,
            "교육,학문 > 학습시설 > 도서관" to PlaceCategory.CULTURE,
            "문화,예술 > 문화시설 > 아쿠아리움" to PlaceCategory.EXPERIENCE,
            "문화,예술 > 미술,공예 > 도자기" to PlaceCategory.EXPERIENCE,
            "문화,예술 > 미술,공예 > 수예,자수" to PlaceCategory.EXPERIENCE,
            "여행 > 관광,명소 > 테마파크" to PlaceCategory.EXPERIENCE,
            "여행 > 관광,명소 > 동물원" to PlaceCategory.EXPERIENCE,
            "여행 > 체험여행" to PlaceCategory.EXPERIENCE,
            "여행 > 공원" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 산" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 계곡" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 등산로" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 숲" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 섬" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 호수" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 하천" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 폭포" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 수목원,식물원" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 도보여행" to PlaceCategory.NATURE,
            "여행 > 관광,명소 > 생태보존,서식지" to PlaceCategory.NATURE,
            "여행 > 숙박 > 야영,캠핑장" to PlaceCategory.NATURE,
            "여행 > 관광,명소" to PlaceCategory.LANDMARK,
            "문화,예술 > 종교 > 불교" to PlaceCategory.LANDMARK,
            "교통,수송 > 도로시설 > 교량,다리" to PlaceCategory.LANDMARK,
            "부동산 > 빌딩" to PlaceCategory.LANDMARK,
            "가정,생활 > 여가시설" to PlaceCategory.ENTERTAINMENT,
            "스포츠,레저" to PlaceCategory.ENTERTAINMENT,
            "가정,생활 > 목욕탕,사우나" to PlaceCategory.WELLNESS,
            "여행 > 관광,명소 > 온천" to PlaceCategory.WELLNESS,
            "스포츠,레저 > 요가,필라테스" to PlaceCategory.WELLNESS,
        ).map { (prefix, category) -> segments(prefix) to category }
            .sortedByDescending { (prefix, _) -> prefix.size }

    fun toPlaceCategory(
        categoryName: String?,
        groupCode: String?,
    ): PlaceCategory {
        val nameSegments = categoryName?.let(::segments).orEmpty()
        return RULES.firstOrNull { (prefix, _) -> nameSegments.take(prefix.size) == prefix }?.second
            ?: when (groupCode) {
                "CE7" -> PlaceCategory.CAFE
                "FD6" -> PlaceCategory.RESTAURANT
                "CT1" -> PlaceCategory.CULTURE
                "AT4" -> PlaceCategory.LANDMARK
                "MT1", "CS2" -> PlaceCategory.SHOPPING
                else -> PlaceCategory.WELLNESS
            }
    }

    private fun segments(categoryName: String): List<String> =
        categoryName.split('>').map(String::trim).filter(String::isNotEmpty)
}
