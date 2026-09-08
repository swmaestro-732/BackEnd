package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceReviewQueryRepository
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import com.example.backend.place.application.port.outbound.PlaceReviewQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewRow
import com.example.backend.place.domain.model.PlaceReviewTag
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlaceReviewQueryPort] 를 구현한다.
 * 실제 테이블 접근은 [PlaceReviewQueryRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class PlaceReviewQueryAdapter(
    private val placeReviewQueryRepository: PlaceReviewQueryRepository,
) : PlaceReviewQueryPort {
    override fun findReviewsByPlace(
        placeId: Long,
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: PlaceReviewCursor?,
        limit: Int,
    ): List<PlaceReviewRow> = placeReviewQueryRepository.findReviewsByPlace(placeId, sort, descending, cursor, limit)

    override fun findPhotoUrls(reviewIds: List<Long>): Map<Long, List<String>> =
        placeReviewQueryRepository.findPhotoUrls(reviewIds)

    override fun findTags(reviewIds: List<Long>): Map<Long, List<PlaceReviewTag>> =
        placeReviewQueryRepository.findTags(reviewIds)

    override fun countReviewsByRating(placeId: Long): Map<Int, Long> =
        placeReviewQueryRepository.countReviewsByRating(placeId)

    override fun countPhotosByPlace(placeId: Long): Long = placeReviewQueryRepository.countPhotosByPlace(placeId)
}
