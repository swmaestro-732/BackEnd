package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.domain.model.PlaceCategory

/**
 * 카카오 category_group_code → 내부 [PlaceCategory] 매핑.
 * 카카오 그룹 코드는 대분류만 있어(음식점·카페·관광명소 등) 우리 카테고리로 1:1 정밀 매핑은 안 된다.
 */
object KakaoCategoryMapper {
    // ponytail: rough kakao group-code→category map, tune with real data
    fun toPlaceCategory(groupCode: String?): PlaceCategory =
        when (groupCode) {
            "CE7" -> PlaceCategory.CAFE
            "FD6" -> PlaceCategory.RESTAURANT
            "CT1" -> PlaceCategory.CULTURE
            "AT4" -> PlaceCategory.LANDMARK
            "MT1" -> PlaceCategory.SHOPPING
            "CS2" -> PlaceCategory.SHOPPING
            "HP8" -> PlaceCategory.WELLNESS
            "PM9" -> PlaceCategory.WELLNESS
            "AD5" -> PlaceCategory.LANDMARK
            else -> PlaceCategory.LANDMARK
        }
}
