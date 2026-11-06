package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.domain.model.PlaceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KakaoCategoryMapperTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "음식점 > 카페 > 테마카페 > 북카페 | CE7 | CAFE",
            "음식점 > 간식 > 제과,베이커리 | FD6 | CAFE",
            "음식점 > 술집 > 와인바 | FD6 | BAR",
            "음식점 > 한식 | FD6 | RESTAURANT",
            "가정,생활 > 시장 | | SHOPPING",
            "가정,생활 > 편의점 > GS25 | CS2 | SHOPPING",
            "가정,생활 > 슈퍼마켓 > 대형슈퍼 > GS더프레시 | MT1 | SHOPPING",
            "교육,학문 > 학습시설 > 도서관 > 작은도서관 | | CULTURE",
            "문화,예술 > 문화시설 > 아쿠아리움 | CT1 | EXPERIENCE",
            "여행 > 공원 > 도시근린공원 | | NATURE",
            "여행 > 관광,명소 > 문화유적 > 고궁,궁 | AT4 | LANDMARK",
            "가정,생활 > 여가시설 > 보드카페 | CE7 | ENTERTAINMENT",
            "스포츠,레저 > 요가,필라테스 > 요가원 | | WELLNESS",
        ],
    )
    fun `category_name 은 가장 구체적인 접두 규칙으로 매핑한다`(
        categoryName: String,
        groupCode: String?,
        expected: PlaceCategory,
    ) {
        assertEquals(expected, KakaoCategoryMapper.toPlaceCategory(categoryName, groupCode))
    }

    @Test
    fun `접두는 단계 단위로 비교한다`() {
        assertEquals(PlaceCategory.ETC, KakaoCategoryMapper.toPlaceCategory("여행 > 공원시설물", null))
    }

    @ParameterizedTest
    @CsvSource(
        "CE7, CAFE",
        "FD6, RESTAURANT",
        "CT1, CULTURE",
        "AT4, LANDMARK",
        "MT1, SHOPPING",
        "CS2, SHOPPING",
        "HP8, ETC",
    )
    fun `category_name 매칭이 없으면 그룹 코드로 매핑한다`(
        groupCode: String,
        expected: PlaceCategory,
    ) {
        assertEquals(expected, KakaoCategoryMapper.toPlaceCategory(null, groupCode))
    }

    @Test
    fun `분류 정보가 없으면 ETC 로 둔다`() {
        assertEquals(PlaceCategory.ETC, KakaoCategoryMapper.toPlaceCategory("", null))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "음식점 > 카페 > 테마카페 > 북카페 | 카페",
            "음식점 > 간식 > 제과,베이커리 | 제과,베이커리",
            "음식점 > 한식 > 육류,고기 | 음식점",
            "여행 > 공원 > 도시근린공원 | 공원",
            "여행 > 관광,명소 > 수목원,식물원 | 수목원,식물원",
            "여행 > 관광,명소 > 문화유적 | 관광,명소",
            "스포츠,레저 > 볼링장 | 스포츠,레저",
        ],
    )
    fun `상세 분류는 매칭된 규칙의 마지막 단계 원문이다`(
        categoryName: String,
        expected: String,
    ) {
        assertEquals(expected, KakaoCategoryMapper.toCategoryDetail(categoryName))
    }

    @Test
    fun `이름 규칙에 걸리지 않으면 상세 분류는 null 이다`() {
        assertNull(KakaoCategoryMapper.toCategoryDetail("여행 > 공원시설물"))
        assertNull(KakaoCategoryMapper.toCategoryDetail(""))
        assertNull(KakaoCategoryMapper.toCategoryDetail(null))
    }
}
