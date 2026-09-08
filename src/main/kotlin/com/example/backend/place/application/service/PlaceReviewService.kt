package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.PlaceReviewUseCase
import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewPersistencePort
import com.example.backend.place.domain.model.PlaceReview
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 리뷰 작성·삭제 유스케이스.
 *
 * 리뷰 대상 장소가 살아 있는지 [PlaceQueryPort] 로 확인한다(없거나 삭제됐으면 404).
 * 태그 코드 → 도메인 enum 변환(모르는 코드는 400)은 웹 어댑터(toCommand)가 맡는다.
 * 별점·사진 개수·한마디 길이 같은 불변식은 [PlaceReview.create] 가 검증한다.
 * 리뷰 본문과 사진·태그 연결은 [PlaceReviewPersistencePort.save] 가 한 트랜잭션에 함께 심는다.
 */
@Service
@Transactional
class PlaceReviewService(
    private val placeQueryPort: PlaceQueryPort,
    private val placeReviewPersistencePort: PlaceReviewPersistencePort,
) : PlaceReviewUseCase {
    override fun create(command: CreatePlaceReviewCommand): PlaceReview {
        requirePlaceExist(command.placeId)

        return placeReviewPersistencePort.save(
            PlaceReview.create(
                placeId = command.placeId,
                userId = command.userId,
                rating = command.rating,
                content = command.content,
                photoUrls = command.photoUrls,
                tags = command.tags,
            ),
        )
    }

    override fun delete(
        userId: Long,
        placeId: Long,
        reviewId: Long,
    ) {
        val deleted = placeReviewPersistencePort.softDelete(reviewId = reviewId, placeId = placeId, userId = userId)
        if (deleted == 0) {
            throw BusinessException(PlaceErrorCode.PLACE_REVIEW_NOT_FOUND)
        }
    }

    private fun requirePlaceExist(placeId: Long) {
        placeQueryPort.findPlaceById(placeId)
            ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
    }
}
