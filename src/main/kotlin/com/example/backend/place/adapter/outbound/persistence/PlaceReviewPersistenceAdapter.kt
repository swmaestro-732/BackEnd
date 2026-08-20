package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceReviewRepository
import com.example.backend.place.application.port.outbound.PlaceReviewPersistencePort
import com.example.backend.place.domain.model.PlaceReview
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlaceReviewPersistencePort] 를 구현한다.
 * 실제 테이블(place_reviews·place_review_photos·place_review_tag_links) 접근은 [PlaceReviewRepository] 에 위임한다.
 */
@Component
class PlaceReviewPersistenceAdapter(
    private val placeReviewRepository: PlaceReviewRepository,
) : PlaceReviewPersistencePort {
    override fun save(review: PlaceReview): PlaceReview = placeReviewRepository.insert(review)
}
