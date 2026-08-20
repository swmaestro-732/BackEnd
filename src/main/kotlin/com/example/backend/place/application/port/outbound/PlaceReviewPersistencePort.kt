package com.example.backend.place.application.port.outbound

import com.example.backend.place.domain.model.PlaceReview

/**
 * 아웃바운드 포트 — 장소 리뷰(place_reviews) 쓰기 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface PlaceReviewPersistencePort {
    /**
     * 리뷰와 자식(사진·태그 연결)을 함께 저장하고, 생성값(id·created_at)까지 채운 [PlaceReview] 를 반환한다.
     * 트랜잭션 경계는 호출하는 서비스가 소유한다.
     */
    fun save(review: PlaceReview): PlaceReview
}
