package com.example.backend.place.application.port.inbound

import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.domain.model.PlaceReview

/**
 * 인바운드 포트 — 장소 리뷰 작성(공개 API).
 *
 * 대상 장소가 실제로 존재하는지 검증하고(없으면 404), 태그 코드를 도메인 태그로 옮긴 뒤
 * 리뷰 본문·사진·태그를 한 트랜잭션에 저장한다.
 * 같은 사용자가 같은 장소에 여러 번 리뷰를 남기는 것은 막지 않는다(재방문마다 작성).
 */
interface PlaceReviewUseCase {
    fun create(command: CreatePlaceReviewCommand): PlaceReview
}
