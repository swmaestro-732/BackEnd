package com.example.backend.mobile.place.application.service

import com.example.backend.mobile.place.application.port.inbound.PlaceReviewScreenUseCase
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenQuery
import com.example.backend.mobile.place.application.port.inbound.dto.PlaceReviewScreenResult
import com.example.backend.place.application.port.inbound.PlaceReviewQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceReviewsQuery
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 후기 전체보기 화면 조합 서비스(BFF). */
@Service
@Transactional(readOnly = true)
class PlaceReviewScreenService(
    private val placeReviewQueryUseCase: PlaceReviewQueryUseCase,
    private val userSummaryUseCase: UserSummaryUseCase,
) : PlaceReviewScreenUseCase {
    override fun getScreen(query: PlaceReviewScreenQuery): PlaceReviewScreenResult {
        val page =
            placeReviewQueryUseCase.getReviews(
                PlaceReviewsQuery(
                    placeId = query.placeId,
                    sort = query.sort,
                    descending = query.descending,
                    cursor = query.cursor,
                    size = query.size,
                ),
            )
        val authors =
            userSummaryUseCase
                .findSummaries(page.reviews.map { it.userId }.distinct())
                .associateBy { it.id }
        // 비로그인은 방문 이력이 있을 수 없으니 무조건 false. 로그인 유저 판정은 실구현에서 채운다.
        val hasVisitedPlace = if (query.viewerId == null) false else STUB_HAS_VISITED_PLACE
        return PlaceReviewScreenResult.of(page, authors, hasVisitedPlace = hasVisitedPlace)
    }

    companion object {
        /** STUB: 방문 이력 판정 전 안전 고정값(false). 방문 처리 실구현 시 제거한다. */
        const val STUB_HAS_VISITED_PLACE = false
    }
}
