package com.example.backend.mobile.place.application.port.inbound

import com.example.backend.mobile.place.application.port.inbound.dto.PlaceDetailScreenResult

/**
 * 장소 상세 화면 조합 (BFF). 장소 정보(place)를 조회해 한 화면 응답 재료를 만든다.
 * 리뷰·이 근처 코스·저장 여부는 아직 백엔드가 없어 빈/false 스텁으로 채운다(MVP 범위).
 */
interface PlaceDetailScreenUseCase {
    /** [placeId] 장소 상세 화면 재료를 조합해 반환한다. 없으면 PLACE_NOT_FOUND. */
    fun getScreen(placeId: Long): PlaceDetailScreenResult
}
