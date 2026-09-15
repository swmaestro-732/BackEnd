package com.example.backend.mobile.place.application.port.inbound

import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenQuery
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenResult

/** 인바운드 포트 — 후기 전체보기 화면 조합(BFF). */
interface PlaceReviewScreenUseCase {
    fun getScreen(query: PlaceReviewScreenQuery): PlaceReviewScreenResult
}
