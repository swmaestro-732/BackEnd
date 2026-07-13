package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.place.application.dto.PlaceDetailResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.outbound.PlaceQueryPort
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 장소 상세 유스케이스 구현.
 * place 영속성 조회에 user 도메인의 [UserSummaryUseCase](inbound 포트 — 크로스 도메인 허용 경로)로
 * 리뷰 작성자 표시 정보를 병합한다.
 */
@Service
@Transactional(readOnly = true)
class PlaceQueryService(
    private val placeQueryPort: PlaceQueryPort,
    private val userSummaryUseCase: UserSummaryUseCase,
    private val clock: Clock,
) : PlaceQueryUseCase {
    override fun getDetail(placeId: Long): PlaceDetailResult {
        val place =
            placeQueryPort.findPlace(placeId)
                ?: throw BusinessException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다: id=$placeId")

        val reviews = placeQueryPort.findRecentReviews(placeId, REVIEW_PREVIEW_COUNT)
        val authors =
            userSummaryUseCase
                .findSummaries(reviews.map { it.userId }.distinct())
                .associateBy { it.id }

        return PlaceDetailResult(
            id = place.id,
            name = place.name,
            categories = listOf(place.category),
            imageUrls = listOfNotNull(place.imageUrl),
            address = place.address,
            latitude = place.latitude,
            longitude = place.longitude,
            openStatus = place.businessStatus.toOpenStatus(),
            openingHoursText = formatOpeningHours(placeQueryPort.findBusinessHours(placeId)),
            reviewSummary =
                PlaceDetailResult.ReviewSummary(
                    averageRating = roundToOneDecimal(placeQueryPort.averageRating(placeId) ?: 0.0),
                    totalCount = placeQueryPort.countReviews(placeId),
                    reviews = reviews.map { it.toResult(authors[it.userId]) },
                ),
            // 인증(JWT) 도입 전까지 사용자 컨텍스트가 없어 false 고정. 도입 시 user 도메인 저장 여부 조회로 교체.
            viewerHasSaved = false,
        )
    }

    private fun PlaceQueryPort.ReviewRecord.toResult(author: UserSummaryUseCase.UserSummary?) =
        PlaceDetailResult.Review(
            id = id,
            authorId = userId,
            authorNickname = author?.nickname ?: WITHDRAWN_USER_NICKNAME,
            authorProfileImageUrl = author?.profileImageUrl,
            rating = rating,
            content = content.orEmpty(),
            createdAt = createdAt,
            relativeTime = formatRelativeTime(createdAt),
            photoUrls = photoUrls,
        )

    private fun PlaceBusinessStatus.toOpenStatus() =
        when (this) {
            PlaceBusinessStatus.OPEN -> PlaceDetailResult.OpenStatus.OPEN
            else -> PlaceDetailResult.OpenStatus.CLOSED
        }

    /** 7일 전체의 영업시간이 동일할 때만 "매일 HH:mm – HH:mm"으로 표기, 아니면 null(요일별 표기는 추후). */
    private fun formatOpeningHours(hours: List<PlaceQueryPort.BusinessHourRecord>): String? {
        if (hours.map { it.dayOfWeek }.distinct().size != DAYS_PER_WEEK) return null
        val spans = hours.map { it.openTime to it.closeTime }.distinct()
        val (open, close) = spans.singleOrNull() ?: return null
        if (open == null || close == null) return null
        return "매일 ${open.format(TIME_FORMAT)} – ${close.format(TIME_FORMAT)}"
    }

    private fun formatRelativeTime(createdAt: java.time.Instant): String {
        val duration = Duration.between(createdAt, clock.instant())
        return when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 30 -> "${duration.toDays()}일 전"
            duration.toDays() < 365 -> "${duration.toDays() / 30}개월 전"
            else -> "${duration.toDays() / 365}년 전"
        }
    }

    private fun roundToOneDecimal(value: Double): Double = (value * 10).roundToInt() / 10.0

    companion object {
        /** 상세 화면 리뷰 미리보기 개수(디자인 확정: 2개, 전체는 리뷰 목록 API). */
        private const val REVIEW_PREVIEW_COUNT = 2
        private const val WITHDRAWN_USER_NICKNAME = "탈퇴한 사용자"
        private const val DAYS_PER_WEEK = 7
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    }
}
