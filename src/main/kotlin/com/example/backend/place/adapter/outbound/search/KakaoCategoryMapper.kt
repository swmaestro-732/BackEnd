package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.domain.model.PlaceCategory

/**
 * 카카오 분류 → 내부 [PlaceCategory] 매핑.
 * `category_name`(대 > 중 > 소분류)을 단계 단위 접두로 매칭하고(가장 긴 접두 우선), 매칭이 없으면 `category_group_code` 로,
 * 그것도 없으면 [PlaceCategory.ETC] 로 둔다.
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
            // 소품샵 성격의 가게(문구·장난감·공예품·레코드·꽃). 화랑·도자기·수예 공방은 더 긴 규칙이 우선한다.
            "가정,생활 > 문구,사무용품" to PlaceCategory.SHOPPING,
            "가정,생활 > 유아 > 장난감,완구" to PlaceCategory.SHOPPING,
            "문화,예술 > 미술,공예" to PlaceCategory.SHOPPING,
            "문화,예술 > 음악 > 음반,레코드샵" to PlaceCategory.SHOPPING,
            "가정,생활 > 꽃집,꽃배달" to PlaceCategory.SHOPPING,
            "가정,생활 > 가구판매" to PlaceCategory.SHOPPING,
            "가정,생활 > 취미 > 취미용품점" to PlaceCategory.SHOPPING,
            "가정,생활 > 드럭스토어" to PlaceCategory.SHOPPING,
            "가정,생활 > 전자제품 > 전자제품판매" to PlaceCategory.SHOPPING,
            "가정,생활 > 식품판매" to PlaceCategory.SHOPPING,
            "가정,생활 > 상가,아케이드" to PlaceCategory.SHOPPING,
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
            "교육,학문 > 교육단체 > 체험학습장" to PlaceCategory.EXPERIENCE,
            "서비스,산업 > 축산업 > 농장,목장" to PlaceCategory.EXPERIENCE,
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
            "여행 > 공원시설물" to PlaceCategory.NATURE,
            "여행 > 관광,명소" to PlaceCategory.LANDMARK,
            "문화,예술 > 종교 > 불교" to PlaceCategory.LANDMARK,
            "교통,수송 > 도로시설 > 교량,다리" to PlaceCategory.LANDMARK,
            "교통,수송 > 도로시설 > 육교" to PlaceCategory.LANDMARK,
            "교통,수송 > 교통시설 > 항구,포구" to PlaceCategory.LANDMARK,
            "교통,수송 > 교통시설 > 선착장,항만시설" to PlaceCategory.LANDMARK,
            "여행 > 관광지부속시설" to PlaceCategory.LANDMARK,
            "문화,예술 > 종교 > 기독교" to PlaceCategory.LANDMARK,
            "문화,예술 > 종교 > 천주교" to PlaceCategory.LANDMARK,
            "부동산 > 빌딩" to PlaceCategory.LANDMARK,
            "가정,생활 > 여가시설" to PlaceCategory.ENTERTAINMENT,
            "스포츠,레저" to PlaceCategory.ENTERTAINMENT,
            "문화,예술 > 사진 > 사진관,포토스튜디오" to PlaceCategory.ENTERTAINMENT,
            "가정,생활 > 목욕탕,사우나" to PlaceCategory.WELLNESS,
            "여행 > 관광,명소 > 온천" to PlaceCategory.WELLNESS,
            "스포츠,레저 > 요가,필라테스" to PlaceCategory.WELLNESS,
            // 숙박(캠핑장은 더 긴 규칙으로 NATURE)·미용·공간대여는 쉼 성격으로 묶는다.
            "여행 > 숙박" to PlaceCategory.WELLNESS,
            "여행 > 숙박부속시설" to PlaceCategory.WELLNESS,
            "가정,생활 > 미용" to PlaceCategory.WELLNESS,
            "서비스,산업 > 전문대행 > 공간대여" to PlaceCategory.WELLNESS,
        ).map { (prefix, category) -> segments(prefix) to category }
            .sortedByDescending { (prefix, _) -> prefix.size }

    fun toPlaceCategory(
        categoryName: String?,
        groupCode: String?,
    ): PlaceCategory =
        matchRule(categoryName)?.second
            ?: when (groupCode) {
                "CE7" -> PlaceCategory.CAFE
                "FD6" -> PlaceCategory.RESTAURANT
                "CT1" -> PlaceCategory.CULTURE
                "AT4" -> PlaceCategory.LANDMARK
                "MT1", "CS2" -> PlaceCategory.SHOPPING
                else -> PlaceCategory.ETC
            }

    /** 매칭된 규칙의 마지막 단계 원문(예: "수목원,식물원"). 이름 규칙에 걸리지 않으면 null. */
    fun toCategoryDetail(categoryName: String?): String? = matchRule(categoryName)?.first?.last()

    private fun matchRule(categoryName: String?): Pair<List<String>, PlaceCategory>? {
        val nameSegments = categoryName?.let(::segments).orEmpty()
        // RULES 는 긴 접두 순 정렬이라 첫 매칭이 가장 구체적인 규칙이다(예: "음식점 > 카페" 가 "음식점" 보다 먼저).
        return RULES.firstOrNull { (prefix, _) -> nameSegments.take(prefix.size) == prefix }
    }

    private fun segments(categoryName: String): List<String> =
        categoryName.split('>').map(String::trim).filter(String::isNotEmpty)
}
