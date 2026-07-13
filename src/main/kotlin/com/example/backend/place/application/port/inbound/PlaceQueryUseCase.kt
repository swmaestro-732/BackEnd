package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.dto.PlaceDetailResult

/** 인바운드 포트 — 장소 조회 유스케이스. */
interface PlaceQueryUseCase {
    /**
     * 장소 상세(장소 + 영업 상태 + 리뷰 미리보기 + 작성자)를 조회한다.
     * @throws com.example.backend.common.exception.BusinessException 장소가 없으면 NOT_FOUND
     */
    fun getDetail(placeId: Long): PlaceDetailResult
}
