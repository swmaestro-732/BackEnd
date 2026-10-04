package com.example.backend.mobile.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.mobile.place.application.port.inbound.PlaceDetailScreenUseCase
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceDetailScreenResult
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenResult
import com.example.backend.mobile.place.application.port.outbound.ScreenPlacePort
import com.example.backend.place.application.port.inbound.PlaceReviewQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 장소 상세 화면 조합 서비스 (BFF). 장소는 자신의 아웃바운드 포트([ScreenPlacePort])로 조회하고,
 * 리뷰 요약은 도메인 리뷰 조회([PlaceReviewQueryUseCase]) 첫 페이지에 작성자 프로필을 병합해 채운다.
 *
 * 이 근처 코스·저장 여부는 아직 백엔드가 없어 웹 응답에서 빈/false 스텁으로 채운다(MVP 범위).
 */
@Service
@Transactional(readOnly = true)
class PlaceDetailScreenService(
    private val screenPlacePort: ScreenPlacePort,
    private val placeReviewQueryUseCase: PlaceReviewQueryUseCase,
    private val userSummaryUseCase: UserSummaryUseCase,
) : PlaceDetailScreenUseCase {
    override fun getScreen(placeId: Long): PlaceDetailScreenResult {
        val place =
            screenPlacePort.findById(placeId)
                ?: throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND, "장소를 찾을 수 없습니다: id=$placeId")
        val reviewPage =
            placeReviewQueryUseCase.getReviews(PlaceReviewsQuery(placeId = placeId, size = REVIEW_PREVIEW_SIZE))
        val reviewAuthors =
            userSummaryUseCase
                .findSummaries(reviewPage.reviews.map { it.userId }.distinct())
                .associateBy { it.id }
        val reviewSummary =
            PlaceReviewScreenResult.of(
                reviewPage,
                reviewAuthors,
                hasVisitedPlace = PlaceReviewScreenService.STUB_HAS_VISITED_PLACE,
            )
        return PlaceDetailScreenResult(
            id = place.id,
            name = place.name,
            category = place.category,
            imageUrl = place.imageUrl,
            latitude = place.latitude,
            longitude = place.longitude,
            address = place.address,
            reviewSummary = reviewSummary,
        )
    }

    private companion object {
        /** 상세 화면 리뷰 미리보기 개수 — 전체 목록은 후기 전체보기 API 가 담당한다. */
        const val REVIEW_PREVIEW_SIZE = 2
    }
}
