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
        requireNoExistingReview(command.placeId, command.userId)

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

    /**
     * 장소 하나에 사용자당 리뷰는 1개 — 이미 있으면 409. 삭제한 리뷰는 세지 않아 다시 쓸 수 있다.
     * 동시 작성 경합은 이 검사를 통과할 수 있어 DB 유니크 인덱스(uq_place_reviews_user_place)가 최종 방어선이다.
     */
    private fun requireNoExistingReview(
        placeId: Long,
        userId: Long,
    ) {
        if (placeReviewPersistencePort.existsActiveReview(placeId = placeId, userId = userId)) {
            throw BusinessException(
                PlaceErrorCode.PLACE_REVIEW_ALREADY_EXISTS,
                "이미 이 장소에 리뷰를 작성했습니다: placeId=$placeId",
            )
        }
    }
}
