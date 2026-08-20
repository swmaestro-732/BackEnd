package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.application.port.inbound.PlaceReviewUseCase
import com.example.backend.place.application.port.inbound.dto.CreatePlaceReviewCommand
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.application.port.outbound.PlaceReviewPersistencePort
import com.example.backend.place.domain.model.PlaceReview
import com.example.backend.place.domain.model.PlaceReviewTag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 리뷰 작성 유스케이스.
 *
 * 리뷰 대상 장소가 살아 있는지 [PlaceQueryPort] 로 확인하고(없거나 삭제됐으면 404),
 * 태그 코드를 도메인 태그([PlaceReviewTag])로 옮긴다(모르는 코드는 400).
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
        // place_reviews.place_id 에는 FK 가 있지만, 삭제된 장소는 FK 로 걸러지지 않아 여기서 함께 확인한다.
        if (placeQueryPort.findPlacesById(listOf(command.placeId)).isEmpty()) {
            throw BusinessException(ErrorCode.PLACE_NOT_FOUND, "리뷰를 작성할 장소를 찾을 수 없습니다: placeId=${command.placeId}")
        }

        val tags =
            command.tagCodes.map { code ->
                PlaceReviewTag.fromCodeOrNull(code)
                    ?: throw BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 리뷰 태그입니다: $code")
            }

        return placeReviewPersistencePort.save(
            PlaceReview.create(
                placeId = command.placeId,
                userId = command.userId,
                rating = command.rating,
                content = command.content,
                photoUrls = command.photoUrls,
                tags = tags,
            ),
        )
    }
}
